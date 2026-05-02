package com.createtreasury.network;

import com.createtreasury.item.CompanyLinkerItem;
import com.createtreasury.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when the player picks a Company or Bank in the CompanyLinkerScreen.
 * The server writes the selection into the held linker item's NBT.
 */
public class SetLinkerSelectionPacket {

    private final String type;
    private final String name;

    public SetLinkerSelectionPacket(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public static void encode(SetLinkerSelectionPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.type);
        buf.writeUtf(pkt.name);
    }

    public static SetLinkerSelectionPacket decode(FriendlyByteBuf buf) {
        return new SetLinkerSelectionPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(SetLinkerSelectionPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Find the linker item in main or off hand
            ItemStack linker = findLinker(player);
            if (linker.isEmpty()) return;

            CompoundTag tag = linker.getOrCreateTag();
            tag.putString(CompanyLinkerItem.TAG_SELECTED_TYPE, pkt.type);
            tag.putString(CompanyLinkerItem.TAG_SELECTED_NAME, pkt.name);
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack findLinker(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() == ModItems.COMPANY_LINKER.get()) return stack;
        }
        return ItemStack.EMPTY;
    }
}
