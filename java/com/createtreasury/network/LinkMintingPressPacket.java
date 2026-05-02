package com.createtreasury.network;

import com.createtreasury.block.MintingPressBlockEntity;
import com.createtreasury.config.TreasuryServerConfig;
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
 * Sent client → server when the player sneak + right-clicks a Minting Press
 * while holding a Company Linker. The server reads the linker's current selection
 * and sets that as the press's linked bank.
 */
public class LinkMintingPressPacket {

    private final BlockPos pressPos;

    public LinkMintingPressPacket(BlockPos pressPos) {
        this.pressPos = pressPos;
    }

    public static void encode(LinkMintingPressPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pressPos);
    }

    public static LinkMintingPressPacket decode(FriendlyByteBuf buf) {
        return new LinkMintingPressPacket(buf.readBlockPos());
    }

    public static void handle(LinkMintingPressPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Verify player holds a Company Linker
            ItemStack linker = findLinker(player);
            if (linker.isEmpty()) return;

            String name = CompanyLinkerItem.getSelectedName(linker);
            String type = CompanyLinkerItem.getSelectedType(linker);

            if (name == null || type == null) {
                player.sendSystemMessage(Component.literal(
                        "§cNo entity selected in the Company Linker. Right-click to configure it."));
                return;
            }

            // Verify the block entity is a Minting Press within reach
            if (pkt.pressPos.distSqr(player.blockPosition()) > 64 * 64) return; // ~8 block range

            BlockEntity be = player.level().getBlockEntity(pkt.pressPos);
            if (!(be instanceof MintingPressBlockEntity press)) {
                player.sendSystemMessage(Component.literal("§cNo Minting Press found at that position."));
                return;
            }

            // Config: enforce max-links-per-bank cap
            if (TreasuryServerConfig.SERVER.enableMaxLinksPerBank.get()) {
                int cap     = TreasuryServerConfig.SERVER.maxLinksPerBank.get();
                int current = MintingPressBlockEntity.getLoadedLinkCount(name);
                // Don't double-count re-linking the same press to the same bank
                String existingLink = press.getLinkedBankName();
                boolean sameBank = name.equals(existingLink);
                if (!sameBank && current >= cap) {
                    player.sendSystemMessage(Component.literal(
                            "§c" + name + " has reached its link limit (" + cap + " press"
                            + (cap == 1 ? "" : "es") + "). Unlink another press first."));
                    return;
                }
            }

            press.setLinkedBank(name);
            player.sendSystemMessage(Component.literal(
                    "§aLinked Minting Press to §f" + name + " §7[" + type + "]"));
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
