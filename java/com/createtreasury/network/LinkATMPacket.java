package com.createtreasury.network;

import com.createtreasury.block.BrassATMBlockEntity;
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
 * Sent client -> server when a player sneak-right-clicks a Brass ATM with a Company Linker.
 * Links (or unlinks) the ATM to the bank stored in the linker's NBT.
 */
public class LinkATMPacket {

    private final BlockPos pos;

    public LinkATMPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(LinkATMPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static LinkATMPacket decode(FriendlyByteBuf buf) {
        return new LinkATMPacket(buf.readBlockPos());
    }

    public static void handle(LinkATMPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Range check
            if (player.blockPosition().distSqr(pkt.pos) > 100) {
                player.sendSystemMessage(Component.literal("§cToo far from the ATM."));
                return;
            }

            BlockEntity be = player.level().getBlockEntity(pkt.pos);
            if (!(be instanceof BrassATMBlockEntity atm)) {
                player.sendSystemMessage(Component.literal("§cNo ATM found at that position."));
                return;
            }

            // Find the Company Linker in the player's hands
            ItemStack linker = findLinker(player);
            if (linker == null) {
                player.sendSystemMessage(Component.literal("§cNo Company Linker found in hands."));
                return;
            }

            String selectedName = CompanyLinkerItem.getSelectedName(linker);
            String selectedType = CompanyLinkerItem.getSelectedType(linker);

            if (selectedName == null) {
                // No selection = unlink
                atm.setLinkedBank(null);
                player.sendSystemMessage(Component.literal("§aATM unlinked."));
                return;
            }

            if (!"Bank".equals(selectedType)) {
                player.sendSystemMessage(Component.literal(
                        "§cATMs can only be linked to a Bank, not a Company. "
                        + "Select a Bank with the Company Linker."));
                return;
            }

            atm.setLinkedBank(selectedName);
            player.sendSystemMessage(Component.literal(
                    "§aATM linked to bank: §f" + selectedName));
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
