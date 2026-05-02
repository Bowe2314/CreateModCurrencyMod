package com.createtreasury.network;

import com.createtreasury.block.BrassCashRegisterBlock;
import com.createtreasury.block.BrassCashRegisterBlockEntity;
import com.createtreasury.economy.CardUtils;
import com.createtreasury.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Sent client -> server when an owner clicks "Collect" to take accumulated
 * coin revenue out of the cash register.
 */
public class CashRegisterCollectPacket {

    private final BlockPos pos;

    public CashRegisterCollectPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(CashRegisterCollectPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static CashRegisterCollectPacket decode(FriendlyByteBuf buf) {
        return new CashRegisterCollectPacket(buf.readBlockPos());
    }

    public static void handle(CashRegisterCollectPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.blockPosition().distSqr(pkt.pos) > 64) return;

            BlockEntity be = player.level().getBlockEntity(pkt.pos);
            if (!(be instanceof BrassCashRegisterBlockEntity cr)) return;

            if (!BrassCashRegisterBlock.isOwner(player, cr.getLinkedCompany())) {
                player.sendSystemMessage(Component.literal("§cYou are not an owner of this register."));
                return;
            }

            long stash = cr.drainStash();
            if (stash <= 0) {
                player.sendSystemMessage(Component.literal("§eNo revenue to collect."));
                // Refresh screen anyway
                TreasuryNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new OpenCashRegisterPacket(pkt.pos, cr, true));
                return;
            }

            // Give coins as physical items (brass > zinc > andesite)
            int bv = CardUtils.brassValue();
            int zv = CardUtils.zincValue();
            int av = CardUtils.andesiteValue();

            long brass    = stash / bv; stash %= bv;
            long zinc     = stash / zv; stash %= zv;
            long andesite = stash / av;

            giveCoins(player, ModItems.BRASS_COIN.get(),    brass);
            giveCoins(player, ModItems.ZINC_COIN.get(),     zinc);
            giveCoins(player, ModItems.ANDESITE_COIN.get(), andesite);

            long total = brass * bv + zinc * zv + andesite * av;
            player.sendSystemMessage(Component.literal(
                    "§aCollected: §f" + CardUtils.formatBalance(total)));

            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenCashRegisterPacket(pkt.pos, cr, true));
        });
        ctx.get().setPacketHandled(true);
    }

    private static void giveCoins(ServerPlayer player, Item item, long count) {
        while (count > 0) {
            int batch = (int) Math.min(count, item.getMaxStackSize());
            ItemStack stack = new ItemStack(item, batch);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            count -= batch;
        }
    }
}
