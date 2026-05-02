package com.createtreasury.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server → client to open the {@link com.createtreasury.gui.CompanyLinkerScreen}.
 * Always sent AFTER the {@link SyncCompanyPacket}s so the cache is populated by the
 * time the screen actually opens.
 */
public class OpenLinkerScreenPacket {

    public static void encode(OpenLinkerScreenPacket pkt, FriendlyByteBuf buf) {
        // no payload
    }

    public static OpenLinkerScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenLinkerScreenPacket();
    }

    public static void handle(OpenLinkerScreenPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OpenLinkerScreenPacket::openScreen)
        );
        ctx.get().setPacketHandled(true);
    }

    private static void openScreen() {
        Minecraft.getInstance().setScreen(new com.createtreasury.gui.CompanyLinkerScreen());
    }
}
