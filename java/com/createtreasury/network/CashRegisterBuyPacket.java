package com.createtreasury.network;

import com.createtreasury.block.BrassCashRegisterBlockEntity;
import com.createtreasury.economy.BankAccountManager;
import com.createtreasury.economy.CardUtils;
import com.createtreasury.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent client -> server when a buyer clicks "Buy" on a slot.
 * Handles both coin and card payment modes.
 */
public class CashRegisterBuyPacket {

    public static final int PAY_COIN = 0;
    public static final int PAY_CARD = 1;

    private final BlockPos pos;
    private final int      slot;
    private final int      paymentMode;

    public CashRegisterBuyPacket(BlockPos pos, int slot, int paymentMode) {
        this.pos         = pos;
        this.slot        = slot;
        this.paymentMode = paymentMode;
    }

    public static void encode(CashRegisterBuyPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeInt(pkt.slot);
        buf.writeInt(pkt.paymentMode);
    }

    public static CashRegisterBuyPacket decode(FriendlyByteBuf buf) {
        return new CashRegisterBuyPacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    public static void handle(CashRegisterBuyPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (pkt.slot < 0 || pkt.slot >= BrassCashRegisterBlockEntity.SHOP_SLOTS) return;
            if (player.blockPosition().distSqr(pkt.pos) > 64) return;

            BlockEntity be = player.level().getBlockEntity(pkt.pos);
            if (!(be instanceof BrassCashRegisterBlockEntity cr)) return;

            ItemStack shopItem = cr.getShopItems()[pkt.slot];
            if (shopItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cThis slot is empty."));
                return;
            }

            long price = cr.getPrices()[pkt.slot];
            if (price <= 0) {
                player.sendSystemMessage(Component.literal("§cNo price set for this item."));
                return;
            }

            boolean paid = switch (pkt.paymentMode) {
                case PAY_COIN -> payWithCoins(player, price, cr);
                case PAY_CARD -> payWithCard(player, price, cr);
                default       -> false;
            };

            if (!paid) return;

            // Give one item to the buyer
            ItemStack toGive = shopItem.copy();
            toGive.setCount(1);
            if (!player.getInventory().add(toGive)) {
                player.drop(toGive, false);
            }

            // Reduce stock
            shopItem.shrink(1);
            cr.setChanged();

            player.sendSystemMessage(Component.literal(
                    "§aBought: §f" + toGive.getHoverName().getString()
                    + " §afor §f" + CardUtils.formatBalance(price)));

            // Refresh the buyer's screen with updated stock
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenCashRegisterPacket(pkt.pos, cr, false));
        });
        ctx.get().setPacketHandled(true);
    }

    // ── Payment helpers ───────────────────────────────────────────────────────

    private static boolean payWithCoins(ServerPlayer player, long price,
                                         BrassCashRegisterBlockEntity cr) {
        int bv = CardUtils.brassValue();
        int zv = CardUtils.zincValue();
        int av = CardUtils.andesiteValue();

        Inventory inv = player.getInventory();

        // Count available coins
        long brassAv = 0, zincAv = 0, andesiteAv = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if      (s.getItem() == ModItems.BRASS_COIN.get())    brassAv    += s.getCount();
            else if (s.getItem() == ModItems.ZINC_COIN.get())     zincAv     += s.getCount();
            else if (s.getItem() == ModItems.ANDESITE_COIN.get()) andesiteAv += s.getCount();
        }

        long total = brassAv * bv + zincAv * zv + andesiteAv * av;
        if (total < price) {
            player.sendSystemMessage(Component.literal(
                    "§cNeed §f" + CardUtils.formatBalance(price)
                    + "§c, have §f" + CardUtils.formatBalance(total) + "§c."));
            return false;
        }

        // Greedy exact change: largest denomination first, round up if needed
        long rem = price;

        long brassT    = Math.min(brassAv,    rem / bv); rem -= brassT * bv;
        long zincT     = Math.min(zincAv,     rem / zv); rem -= zincT  * zv;
        long andesiteT = Math.min(andesiteAv, rem / av); rem -= andesiteT * av;

        // rem > 0 means we need to round up with the next smallest available coin
        if (rem > 0) {
            if (andesiteAv > andesiteT) {
                andesiteT++;
                rem -= av;
            } else if (zincAv > zincT) {
                zincT++;
                rem -= zv;
            } else {
                brassT++;
                rem -= bv;
            }
        }

        long change = -rem; // rem is now <= 0, change is the overpayment

        // Remove coins from the inventory
        removeCoins(inv, ModItems.BRASS_COIN.get(),    brassT);
        removeCoins(inv, ModItems.ZINC_COIN.get(),     zincT);
        removeCoins(inv, ModItems.ANDESITE_COIN.get(), andesiteT);

        // Return change in smallest denominations
        if (change > 0) {
            long cB = change / bv; change %= bv;
            long cZ = change / zv; change %= zv;
            long cA = change / av;
            giveCoins(player, ModItems.BRASS_COIN.get(),    cB);
            giveCoins(player, ModItems.ZINC_COIN.get(),     cZ);
            giveCoins(player, ModItems.ANDESITE_COIN.get(), cA);
        }

        cr.addToStash(price);
        return true;
    }

    private static boolean payWithCard(ServerPlayer player, long price,
                                        BrassCashRegisterBlockEntity cr) {
        // Find active card
        ItemStack card = ItemStack.EMPTY;
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(h);
            if (CardUtils.isActive(s)) { card = s; break; }
        }
        if (card.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo active debit card in hand."));
            return false;
        }
        if (CardUtils.isLocked(card)) {
            player.sendSystemMessage(Component.literal("§cYour card is locked."));
            return false;
        }

        UUID holder = CardUtils.getHolderUUID(card);
        if (holder == null || !holder.equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cThis card does not belong to you."));
            return false;
        }

        String bank = CardUtils.getIssuedBy(card);
        if (bank == null) {
            player.sendSystemMessage(Component.literal("§cCard has no associated bank."));
            return false;
        }

        BankAccountManager mgr = BankAccountManager.get(player.getServer());
        mgr.ensureAccount(bank, player.getUUID());
        long balance = mgr.getBalance(bank, player.getUUID());

        if (balance < price) {
            player.sendSystemMessage(Component.literal(
                    "§cInsufficient funds. Need §f" + CardUtils.formatBalance(price)
                    + "§c, balance §f" + CardUtils.formatBalance(balance) + "§c."));
            return false;
        }

        mgr.withdraw(bank, player.getUUID(), price);
        cr.addToStash(price);
        return true;
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    private static void removeCoins(Inventory inv, Item coinItem, long count) {
        for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == coinItem) {
                int take = (int) Math.min(count, s.getCount());
                s.shrink(take);
                count -= take;
            }
        }
    }

    private static void giveCoins(ServerPlayer player, Item item, long count) {
        while (count > 0) {
            int batch = (int) Math.min(count, item.getMaxStackSize());
            ItemStack stack = new ItemStack(item, batch);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            count -= batch;
        }
    }
}
