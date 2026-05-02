package com.createtreasury.network;

import com.createtreasury.company.Company;
import com.createtreasury.company.CompanyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AcceptInvitePacket {

    public static void encode(AcceptInvitePacket pkt, FriendlyByteBuf buf) {}
    public static AcceptInvitePacket decode(FriendlyByteBuf buf) { return new AcceptInvitePacket(); }

    public static void handle(AcceptInvitePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            CompanyManager mgr = CompanyManager.get(player.getServer());
            // acceptInvite now returns the Company so we know its type
            Company c = mgr.acceptInvite(player.getUUID());

            if (c == null) {
                player.sendSystemMessage(Component.literal("No pending invite found."));
                return;
            }

            player.sendSystemMessage(Component.literal("You joined \"" + c.getName() + "\"!"));

            List<String> memberNames = c.getMembers().stream()
                    .map(uuid -> {
                        ServerPlayer p = player.getServer().getPlayerList().getPlayer(uuid);
                        return p != null ? p.getName().getString() : uuid.toString().substring(0, 8) + "…";
                    })
                    .collect(Collectors.toList());

            ServerPlayer owner = player.getServer().getPlayerList().getPlayer(c.getOwnerUUID());
            String ownerName = owner != null ? owner.getName().getString()
                    : c.getOwnerUUID().toString().substring(0, 8) + "…";

            // Sync the new member pendingFrom=null clears the pending invite on the client
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncCompanyPacket(c.getName(), c.getTagline(), c.getIndustry(),
                            c.getType(), false, ownerName, memberNames, null)
            );

            // Notify and sync the owner if online
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(
                        player.getName().getString() + " accepted your invite and joined the " + c.getType() + "!"));
                TreasuryNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> owner),
                        new SyncCompanyPacket(c.getName(), c.getTagline(), c.getIndustry(),
                                c.getType(), true, owner.getName().getString(), memberNames, null)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
