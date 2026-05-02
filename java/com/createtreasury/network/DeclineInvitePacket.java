package com.createtreasury.network;

import com.createtreasury.company.CompanyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public class DeclineInvitePacket {

    public static void encode(DeclineInvitePacket pkt, FriendlyByteBuf buf) {}
    public static DeclineInvitePacket decode(FriendlyByteBuf buf) { return new DeclineInvitePacket(); }

    public static void handle(DeclineInvitePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            CompanyManager mgr = CompanyManager.get(player.getServer());
            mgr.declineInvite(player.getUUID());

            player.sendSystemMessage(Component.literal("Invite declined."));

            // type=null → only clears pendingInviteFrom on the client; company/bank slots untouched
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncCompanyPacket(null, null, null, null, false, null, List.of(), null)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
