package com.createtreasury.network;

import com.createtreasury.block.BrassCashRegisterBlockEntity;
import com.createtreasury.item.CompanyLinkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Sent client -> server when a player sneak-right-clicks a Brass Cash Register
 * with a Company Linker.  Only "Company" type selections are accepted
 * (banks cannot own a shop).
 */
public class LinkCashRegisterPacket {

    private final BlockPos pos;

    public LinkCashRegisterPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(LinkCashRegisterPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static LinkCashRegisterPacket decode(FriendlyByteBuf buf) {
        return new LinkCashRegisterPacket(buf.readBlockPos());
    }

    public static void handle(LinkCashRegisterPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (player.blockPosition().distSqr(pkt.pos) > 100) {
                player.sendSystemMessage(Component.literal("§cToo far from the register."));
                return;
            }

            BlockEntity be = player.level().getBlockEntity(pkt.pos);
            if (!(be instanceof BrassCashRegisterBlockEntity cr)) {
                player.sendSystemMessage(Component.literal("§cNo cash register at that position."));
                return;
            }

            ItemStack linker = findLinker(player);
            if (linker == null) {
                player.sendSystemMessage(Component.literal("§cNo Company Linker found in hand."));
                return;
            }

            String selectedName = CompanyLinkerItem.getSelectedName(linker);
            String selectedType = CompanyLinkerItem.getSelectedType(linker);

            if (selectedName == null) {
                // Empty selection = unlink
                cr.setLinkedCompany(null);
                player.sendSystemMessage(Component.literal("§aCash register unlinked."));
                return;
            }

            if (!"Company".equals(selectedType)) {
                player.sendSystemMessage(Component.literal(
                        "§cCash registers can only be linked to a Company, not a Bank. "
                        + "Select a Company with the Company Linker."));
                return;
            }

            cr.setLinkedCompany(selectedName);
            player.sendSystemMessage(Component.literal(
                    "§aCash register linked to company: §f" + selectedName));
        });
        ctx.get().setPacketHandled(true);
    }

    @Nullable
    private static ItemStack findLinker(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof CompanyLinkerItem) return stack;
        }
        return null;
    }
}
