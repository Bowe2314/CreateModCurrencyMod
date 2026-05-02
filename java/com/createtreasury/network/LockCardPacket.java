package com.createtreasury.network;

import com.createtreasury.economy.CardUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when the player exhausts their PIN attempts.
 * The server sets TAG_LOCKED=true on the active card so it cannot be used at ATMs.
 */
public class LockCardPacket {

    public LockCardPacket() {}

    public static void encode(LockCardPacket pkt, FriendlyByteBuf buf) {}

    public static LockCardPacket decode(FriendlyByteBuf buf) {
        return new LockCardPacket();
    }

    public static void handle(LockCardPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack card = findActiveCard(player);
            if (card.isEmpty()) return;

            card.getOrCreateTag().putBoolean(CardUtils.TAG_LOCKED, true);
            player.sendSystemMessage(Component.literal(
                    "§cYour card has been locked due to too many incorrect PIN attempts. "
                    + "Contact your bank to have it unlocked."));
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
