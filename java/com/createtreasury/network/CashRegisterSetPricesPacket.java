package com.createtreasury.network;

import com.createtreasury.block.BrassCashRegisterBlock;
import com.createtreasury.block.BrassCashRegisterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Sent client -> server when an owner saves the prices for all three slots.
 */
public class CashRegisterSetPricesPacket {

    private final BlockPos pos;
    private final long[]   prices; // length == SHOP_SLOTS

    public CashRegisterSetPricesPacket(BlockPos pos, long[] prices) {
        this.pos    = pos;
        this.prices = prices;
    }

    public static void encode(CashRegisterSetPricesPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        for (long p : pkt.prices) buf.writeLong(p);
    }

    public static CashRegisterSetPricesPacket decode(FriendlyByteBuf buf) {
        BlockPos pos    = buf.readBlockPos();
        long[]   prices = new long[BrassCashRegisterBlockEntity.SHOP_SLOTS];
        for (int i = 0; i < prices.length; i++) prices[i] = buf.readLong();
        return new CashRegisterSetPricesPacket(pos, prices);
    }

    public static void handle(CashRegisterSetPricesPacket pkt, Supplier<NetworkEvent.Context> ctx) {
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

            cr.setPrices(pkt.prices);
            player.sendSystemMessage(Component.literal("§aPrices saved."));

            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenCashRegisterPacket(pkt.pos, cr, true));
        });
        ctx.get().setPacketHandled(true);
    }
}
