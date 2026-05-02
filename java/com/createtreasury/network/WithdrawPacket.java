package com.createtreasury.network;

import com.createtreasury.block.BrassATMBlockEntity;
import com.createtreasury.config.TreasuryServerConfig;
import com.createtreasury.economy.ATMSessionTracker;
import com.createtreasury.economy.BankAccountManager;
import com.createtreasury.economy.CardUtils;
import com.createtreasury.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent client -> server when the player requests a coin withdrawal.
 * Carries the denomination type and quantity so the server gives exactly
 * that many coins of that type - no decomposition guesswork.
 */
public class WithdrawPacket {

    public static final int DENOM_ANDESITE = 0;
    public static final int DENOM_ZINC     = 1;
    public static final int DENOM_BRASS    = 2;

    private final long qty;   // number of coins requested
    private final int  denom; // DENOM_* constant

    public WithdrawPacket(long qty, int denom) {
        this.qty   = qty;
        this.denom = denom;
    }

    public static void encode(WithdrawPacket pkt, FriendlyByteBuf buf) {
        buf.writeLong(pkt.qty);
        buf.writeInt(pkt.denom);
    }

    public static WithdrawPacket decode(FriendlyByteBuf buf) {
        return new WithdrawPacket(buf.readLong(), buf.readInt());
    }

    public static void handle(WithdrawPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (pkt.qty <= 0) return;
            if (pkt.denom < DENOM_ANDESITE || pkt.denom > DENOM_BRASS) return;

            // Find active card
            ItemStack card = findActiveCard(player);
            if (card.isEmpty()) return;

            if (CardUtils.isLocked(card)) {
                player.sendSystemMessage(Component.literal("§cYour card is locked. Contact your bank."));
                return;
            }

            UUID holderUUID = CardUtils.getHolderUUID(card);
            if (holderUUID == null || !holderUUID.equals(player.getUUID())) return;

            String bank = CardUtils.getIssuedBy(card);
            if (bank == null) return;

            BankAccountManager mgr = BankAccountManager.get(player.getServer());
            mgr.ensureAccount(bank, player.getUUID());
            long currentBalance = mgr.getBalance(bank, player.getUUID());

            // Resolve denomination
            int bv = CardUtils.brassValue();
            int zv = CardUtils.zincValue();
            int av = CardUtils.andesiteValue();

            int denomValue;
            Item coinItem;
            switch (pkt.denom) {
                case DENOM_BRASS    -> { denomValue = bv; coinItem = ModItems.BRASS_COIN.get();    }
                case DENOM_ZINC     -> { denomValue = zv; coinItem = ModItems.ZINC_COIN.get();     }
                default             -> { denomValue = av; coinItem = ModItems.ANDESITE_COIN.get(); }
            }

            // How many whole coins can the player actually afford (including fee)?
            double feePercent = TreasuryServerConfig.SERVER.withdrawalFeePercent.get();
            double effectiveRate = 1.0 + feePercent / 100.0;
            long maxAffordable = (long)(currentBalance / (denomValue * effectiveRate));
            long coinsToGive   = Math.min(pkt.qty, maxAffordable);

            if (coinsToGive <= 0) {
                player.sendSystemMessage(Component.literal(
                        "§cInsufficient funds for even 1 "
                        + coinItem.getDescription().getString() + "§c."));
                sendBalanceUpdate(player, bank, mgr, card);
                return;
            }

            long actualCost    = coinsToGive * (long) denomValue;
            long fee           = (long)(actualCost * feePercent / 100.0);
            long totalDeducted = actualCost + fee;

            // Enforce configured min / max limits (in base units)
            long minW = TreasuryServerConfig.SERVER.minWithdrawal.get();
            long maxW = TreasuryServerConfig.SERVER.maxWithdrawalPerTx.get();
            if (actualCost < minW) {
                player.sendSystemMessage(Component.literal(
                        "§cMinimum withdrawal is §f" + CardUtils.formatBalance(minW) + "§c."));
                return;
            }
            if (maxW > 0 && actualCost > maxW) {
                player.sendSystemMessage(Component.literal(
                        "§cMaximum withdrawal per transaction is §f"
                        + CardUtils.formatBalance(maxW) + "§c."));
                return;
            }

            if (!mgr.withdraw(bank, player.getUUID(), totalDeducted)) {
                player.sendSystemMessage(Component.literal("§cInsufficient funds."));
                sendBalanceUpdate(player, bank, mgr, card);
                return;
            }

            if (fee > 0) {
                player.sendSystemMessage(Component.literal(
                        "§7Fee deducted: §f" + CardUtils.formatBalance(fee)));
            }

            // Tag coins with the ATM's linked bank if applicable
            BrassATMBlockEntity atmBE = ATMSessionTracker.getATM(player);
            String mintedBy = (atmBE != null) ? atmBE.getLinkedBank() : null;

            giveItem(player, coinItem, coinsToGive, mintedBy);
            sendBalanceUpdate(player, bank, mgr, card);
        });
        ctx.get().setPacketHandled(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ItemStack findActiveCard(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (CardUtils.isActive(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void giveItem(ServerPlayer player, Item item, long count,
                                  @Nullable String mintedBy) {
        while (count > 0) {
            int batch = (int) Math.min(count, item.getMaxStackSize());
            ItemStack stack = new ItemStack(item, batch);
            if (mintedBy != null && !mintedBy.isEmpty()) {
                stack.getOrCreateTag().putString("MintedBy", mintedBy);
            }
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            count -= batch;
        }
    }

    private static void sendBalanceUpdate(ServerPlayer player, String bank,
                                          BankAccountManager mgr, ItemStack card) {
        long newBalance = mgr.getBalance(bank, player.getUUID());
        boolean hasPin  = CardUtils.hasPin(card);
        TreasuryNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenBankingScreenPacket(bank, newBalance, hasPin));
    }
}
