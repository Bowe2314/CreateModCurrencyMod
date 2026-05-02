package com.createtreasury.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server → client to open (or refresh) the banking screen.
 * Carries the player's current balance and whether a PIN is required to proceed.
 */
public class OpenBankingScreenPacket {

    private final String  bankName;
    private final long    balance;     // in base units (zinc = 1)
    private final boolean requirePin;  // true if the card has a PIN set

    public OpenBankingScreenPacket(String bankName, long balance, boolean requirePin) {
        this.bankName   = bankName;
        this.balance    = balance;
        this.requirePin = requirePin;
    }

    public static void encode(OpenBankingScreenPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.bankName, 128);
        buf.writeLong(pkt.balance);
        buf.writeBoolean(pkt.requirePin);
    }

    public static OpenBankingScreenPacket decode(FriendlyByteBuf buf) {
        String  bankName   = buf.readUtf(128);
        long    balance    = buf.readLong();
        boolean requirePin = buf.readBoolean();
        return new OpenBankingScreenPacket(bankName, balance, requirePin);
    }

    public static void handle(OpenBankingScreenPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    openOrRefresh(pkt.bankName, pkt.balance, pkt.requirePin)));
        ctx.get().setPacketHandled(true);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(Dist.CLIENT)
    private static void openOrRefresh(String bankName, long balance, boolean requirePin) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof com.createtreasury.gui.BankingScreen bankingScreen) {
            // Already open just update balance display
            bankingScreen.updateBalance(balance);
        } else {
            mc.setScreen(new com.createtreasury.gui.BankingScreen(bankName, balance, requirePin));
        }
    }
}
