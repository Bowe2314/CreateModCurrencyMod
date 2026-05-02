package com.createtreasury.network;

import com.createtreasury.block.CardPressBlockEntity;
import com.createtreasury.item.CompanyLinkerItem;
import com.createtreasury.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when the player sneak + right-clicks a Card Press
 * while holding a Company Linker. Sets the press's linked bank.
 */
public class LinkCardPressPacket {

    private final BlockPos pressPos;

    public LinkCardPressPacket(BlockPos pressPos) {
        this.pressPos = pressPos;
    }

    public static void encode(LinkCardPressPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pressPos);
    }

    public static LinkCardPressPacket decode(FriendlyByteBuf buf) {
        return new LinkCardPressPacket(buf.readBlockPos());
    }

    public static void handle(LinkCardPressPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack linker = findLinker(player);
            if (linker.isEmpty()) return;

            String name = CompanyLinkerItem.getSelectedName(linker);
            String type = CompanyLinkerItem.getSelectedType(linker);

            if (name == null || type == null) {
                player.sendSystemMessage(Component.literal(
                        "§cNo entity selected in the Company Linker. Right-click to configure it."));
                return;
            }

            if (pkt.pressPos.distSqr(player.blockPosition()) > 64 * 64) return;

            BlockEntity be = player.level().getBlockEntity(pkt.pressPos);
            if (!(be instanceof CardPressBlockEntity press)) {
                player.sendSystemMessage(Component.literal("§cNo Card Press found at that position."));
                return;
            }

            press.setLinkedBank(name);
            player.sendSystemMessage(Component.literal(
                    "§aLinked Card Press to §f" + name + " §7[" + type + "]"));
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
