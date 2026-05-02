package com.createtreasury.network;

import com.createtreasury.block.BrassATMBlockEntity;
import com.createtreasury.config.TreasuryServerConfig;
import com.createtreasury.economy.ATMSessionTracker;
import com.createtreasury.economy.BankAccountManager;
import com.createtreasury.economy.CardUtils;
import com.createtreasury.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent client -> server when the player clicks "Deposit All" at the ATM.
 * Scans the player's inventory for coins, removes them, and adds their
 * value to the account balance.
 *
 * If the ATM is linked to a bank, only coins stamped with "MintedBy: <bankName>"
 * are accepted; untagged or foreign-bank coins are left in the inventory.
 */
public class DepositPacket {

    public DepositPacket() {}

    public static void encode(DepositPacket pkt, FriendlyByteBuf buf) {}

    public static DepositPacket decode(FriendlyByteBuf buf) {
        return new DepositPacket();
    }

    public static void handle(DepositPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

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

            // Check if this ATM is linked to a specific bank (for coin filtering)
            BrassATMBlockEntity atmBE = ATMSessionTracker.getATM(player);
            String linkedBank = (atmBE != null) ? atmBE.getLinkedBank() : null;

            // Scan inventory and collect qualifying coins
            long total = 0;
            int skipped = 0; // coins from wrong bank (for linked ATM message)
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) continue;

                long valuePerItem = 0;
                if (stack.getItem() == ModItems.ZINC_COIN.get())          valuePerItem = CardUtils.zincValue();
                else if (stack.getItem() == ModItems.BRASS_COIN.get())    valuePerItem = CardUtils.brassValue();
                else if (stack.getItem() == ModItems.ANDESITE_COIN.get()) valuePerItem = CardUtils.andesiteValue();

                if (valuePerItem == 0) continue; // not a coin

                // If ATM is linked, only accept coins tagged from that bank
                if (linkedBank != null) {
                    CompoundTag stackTag = stack.getTag();
                    String mintedBy = (stackTag != null) ? stackTag.getString("MintedBy") : "";
                    if (!linkedBank.equals(mintedBy)) {
                        skipped += stack.getCount();
                        continue;
                    }
                }

                total += valuePerItem * stack.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }

            if (total == 0) {
                if (skipped > 0) {
                    player.sendSystemMessage(Component.literal(
                            "§eThis ATM only accepts coins issued by §f" + linkedBank
                            + "§e. Your coins were not accepted."));
                } else {
                    player.sendSystemMessage(Component.literal("§eNo coins found in your inventory."));
                }
                return;
            }

            // Apply deposit fee
            double feePercent = TreasuryServerConfig.SERVER.depositFeePercent.get();
            long fee = (long) (total * feePercent / 100.0);
            long toCredit = total - fee;

            if (toCredit <= 0) {
                player.sendSystemMessage(Component.literal(
                        "§cDeposit fee (§f" + CardUtils.formatBalance(fee)
                        + "§c) consumed the entire deposit."));
                return;
            }

            BankAccountManager mgr = BankAccountManager.get(player.getServer());
            mgr.ensureAccount(bank, player.getUUID());
            long credited = mgr.addBalance(bank, player.getUUID(), toCredit);
            long capped   = toCredit - credited;

            StringBuilder msg = new StringBuilder("§aDeposited §f")
                    .append(CardUtils.formatBalance(total)).append("§a.");
            if (fee > 0)    msg.append(" §7Fee: §f").append(CardUtils.formatBalance(fee)).append("§7.");
            if (capped > 0) msg.append(" §e").append(CardUtils.formatBalance(capped))
                    .append(" rejected (account full).");
            if (skipped > 0) msg.append(" §7(").append(skipped).append(" foreign coin(s) left in inventory.)");
            player.sendSystemMessage(Component.literal(msg.toString()));

            long newBalance = mgr.getBalance(bank, player.getUUID());
            boolean hasPin = CardUtils.hasPin(card);
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenBankingScreenPacket(bank, newBalance, hasPin));
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack findActiveCard(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (CardUtils.isActive(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
