package com.createtreasury.network;

import com.createtreasury.company.CompanyManager;
import com.createtreasury.economy.CardUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client to server when a bank member right-clicks a locked card they issued.
 * Server verifies membership before clearing the lock.
 */
public class UnlockCardPacket {

    public UnlockCardPacket() {}

    public static void encode(UnlockCardPacket pkt, FriendlyByteBuf buf) {}

    public static UnlockCardPacket decode(FriendlyByteBuf buf) {
        return new UnlockCardPacket();
    }

    public static void handle(UnlockCardPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack card = findLockedCard(player);
            if (card.isEmpty()) return;

            String issuedBy = CardUtils.getIssuedBy(card);
            if (issuedBy == null) return;

            CompanyManager mgr = CompanyManager.get(player.getServer());
            if (mgr.getEntityByPlayer(player.getUUID(), "Bank") == null ||
                !issuedBy.equals(mgr.getEntityByPlayer(player.getUUID(), "Bank").getName())) {
                player.sendSystemMessage(Component.literal(
                        "§cYou are not a member of the bank that issued this card."));
                return;
            }

            card.getOrCreateTag().putBoolean(CardUtils.TAG_LOCKED, false);
            card.getOrCreateTag().putInt(CardUtils.TAG_PIN_ATTEMPTS, 0);
            player.sendSystemMessage(Component.literal("§aCard unlocked successfully."));
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack findLockedCard(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (CardUtils.isLocked(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
