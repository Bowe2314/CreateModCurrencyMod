package com.createtreasury.network;

import com.createtreasury.block.BrassCashRegisterBlock;
import com.createtreasury.block.BrassCashRegisterBlockEntity;
import com.createtreasury.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Sent client -> server when an owner clicks "Set" or "Clear" on a shop slot.
 * <p>
 * If the player's main hand is non-empty the entire stack is stored as stock
 * for that slot (coins and cards are rejected).
 * If the main hand is empty the current stock is returned to the player and
 * the slot is cleared.
 */
public class CashRegisterSetSlotPacket {

    private final BlockPos pos;
    private final int      slot;

    public CashRegisterSetSlotPacket(BlockPos pos, int slot) {
        this.pos  = pos;
        this.slot = slot;
    }

    public static void encode(CashRegisterSetSlotPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeInt(pkt.slot);
    }

    public static CashRegisterSetSlotPacket decode(FriendlyByteBuf buf) {
        return new CashRegisterSetSlotPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(CashRegisterSetSlotPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (pkt.slot < 0 || pkt.slot >= BrassCashRegisterBlockEntity.SHOP_SLOTS) return;
            if (player.blockPosition().distSqr(pkt.pos) > 64) return;

            BlockEntity be = player.level().getBlockEntity(pkt.pos);
            if (!(be instanceof BrassCashRegisterBlockEntity cr)) return;

            if (!BrassCashRegisterBlock.isOwner(player, cr.getLinkedCompany())) {
                player.sendSystemMessage(Component.literal("§cYou are not an owner of this register."));
                return;
            }

            ItemStack mainhand = player.getMainHandItem();

            if (mainhand.isEmpty()) {
                // Return stock and clear the slot
                ItemStack current = cr.getShopItems()[pkt.slot];
                if (!current.isEmpty()) {
                    ItemStack toReturn = current.copy();
                    if (!player.getInventory().add(toReturn)) {
                        player.drop(toReturn, false);
                    }
                    cr.setShopItem(pkt.slot, ItemStack.EMPTY);
                    player.sendSystemMessage(Component.literal(
                            "§aSlot " + (pkt.slot + 1) + " cleared — stock returned."));
                }
            } else {
                // Reject coins and debit cards
                Item item = mainhand.getItem();
                if (item == ModItems.BRASS_COIN.get()
                        || item == ModItems.ZINC_COIN.get()
                        || item == ModItems.ANDESITE_COIN.get()
                        || item == ModItems.DEBIT_CARD.get()) {
                    player.sendSystemMessage(Component.literal("§cYou cannot sell coins or cards."));
                    return;
                }

                // Store entire stack as new stock; take it from the player's hand
                ItemStack toStore = mainhand.copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                cr.setShopItem(pkt.slot, toStore);
                player.sendSystemMessage(Component.literal(
                        "§aSlot " + (pkt.slot + 1) + " stocked: §f"
                        + toStore.getCount() + "x " + toStore.getHoverName().getString()));
            }

            // Refresh the owner's screen
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenCashRegisterPacket(pkt.pos, cr, true));
        });
        ctx.get().setPacketHandled(true);
    }
}
