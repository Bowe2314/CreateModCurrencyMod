package com.createtreasury.network;

import com.createtreasury.block.BrassCashRegisterBlockEntity;
import com.createtreasury.gui.CashRegisterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server -> client to open or refresh the Cash Register GUI.
 * Carries a full snapshot of the register's current state.
 */
public class OpenCashRegisterPacket {

    private final BlockPos   pos;
    private final ItemStack[] shopItems;
    private final long[]      prices;
    private final long        coinStash;
    private final String      linkedCompany; // empty string means null
    private final boolean     isOwner;

    /** Server-side constructor: reads directly from the block entity. */
    public OpenCashRegisterPacket(BlockPos pos, BrassCashRegisterBlockEntity be, boolean isOwner) {
        this.pos    = pos;
        int slots   = BrassCashRegisterBlockEntity.SHOP_SLOTS;
        this.shopItems = new ItemStack[slots];
        this.prices    = new long[slots];
        for (int i = 0; i < slots; i++) {
            this.shopItems[i] = be.getShopItems()[i].copy();
            this.prices[i]    = be.getPrices()[i];
        }
        this.coinStash      = be.getCoinStash();
        this.linkedCompany  = be.getLinkedCompany() != null ? be.getLinkedCompany() : "";
        this.isOwner        = isOwner;
    }

    private OpenCashRegisterPacket(BlockPos pos, ItemStack[] shopItems, long[] prices,
                                    long coinStash, String linkedCompany, boolean isOwner) {
        this.pos           = pos;
        this.shopItems     = shopItems;
        this.prices        = prices;
        this.coinStash     = coinStash;
        this.linkedCompany = linkedCompany;
        this.isOwner       = isOwner;
    }

    public static void encode(OpenCashRegisterPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        for (int i = 0; i < BrassCashRegisterBlockEntity.SHOP_SLOTS; i++) {
            buf.writeItem(pkt.shopItems[i]);
            buf.writeLong(pkt.prices[i]);
        }
        buf.writeLong(pkt.coinStash);
        buf.writeUtf(pkt.linkedCompany, 64);
        buf.writeBoolean(pkt.isOwner);
    }

    public static OpenCashRegisterPacket decode(FriendlyByteBuf buf) {
        BlockPos   pos    = buf.readBlockPos();
        int        slots  = BrassCashRegisterBlockEntity.SHOP_SLOTS;
        ItemStack[] items  = new ItemStack[slots];
        long[]      prices = new long[slots];
        for (int i = 0; i < slots; i++) {
            items[i]  = buf.readItem();
            prices[i] = buf.readLong();
        }
        long    stash   = buf.readLong();
        String  company = buf.readUtf(64);
        boolean owner   = buf.readBoolean();
        return new OpenCashRegisterPacket(pos, items, prices, stash, company, owner);
    }

    public static void handle(OpenCashRegisterPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> openScreen(pkt)));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreen(OpenCashRegisterPacket pkt) {
        String company = pkt.linkedCompany.isEmpty() ? null : pkt.linkedCompany;
        Minecraft.getInstance().setScreen(new CashRegisterScreen(
                pkt.pos, pkt.shopItems, pkt.prices,
                pkt.coinStash, company, pkt.isOwner));
    }
}
