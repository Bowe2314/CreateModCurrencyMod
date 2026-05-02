package com.createtreasury.network;

import com.createtreasury.company.Company;
import com.createtreasury.company.CompanyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class LeaveCompanyPacket {

    private final String entityType; // "Company" or "Bank"

    public LeaveCompanyPacket(String entityType) { this.entityType = entityType; }

    public static void encode(LeaveCompanyPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.entityType);
    }

    public static LeaveCompanyPacket decode(FriendlyByteBuf buf) {
        return new LeaveCompanyPacket(buf.readUtf());
    }

    public static void handle(LeaveCompanyPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            CompanyManager mgr     = CompanyManager.get(player.getServer());
            Company        company = mgr.getEntityByPlayer(player.getUUID(), pkt.entityType);

            if (company == null) {
                player.sendSystemMessage(Component.literal("You are not in a " + pkt.entityType + "."));
                return;
            }

            String companyName = company.getName();

            // Preserve the player's pending invite across the leave/disband
            String preservedPending = mgr.getPendingInviteName(player.getUUID());

            if (company.getOwnerUUID().equals(player.getUUID())) {
                // Owner disband entirely, notify all members
                for (UUID memberId : company.getMembers()) {
                    ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                    if (member == null) continue;
                    // Preserve each member's own pending invite
                    String memberPending = mgr.getPendingInviteName(memberId);
                    member.sendSystemMessage(Component.literal(
                            "\"" + companyName + "\" has been disbanded by its owner."));
                    TreasuryNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> member),
                            new SyncCompanyPacket(null, null, null, pkt.entityType,
                                    false, null, List.of(), memberPending));
                }
                mgr.disbandCompany(player.getUUID(), pkt.entityType);
                player.sendSystemMessage(Component.literal("You disbanded \"" + companyName + "\"."));
            } else {
                mgr.leaveCompany(player.getUUID(), pkt.entityType);
                player.sendSystemMessage(Component.literal("You left \"" + companyName + "\"."));

                // Notify owner if online
                ServerPlayer owner = player.getServer().getPlayerList().getPlayer(company.getOwnerUUID());
                if (owner != null) {
                    owner.sendSystemMessage(Component.literal(
                            player.getName().getString() + " left your " + pkt.entityType + "."));
                }
            }

            // Clear the leaving player's slot; preserve their pending invite
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncCompanyPacket(null, null, null, pkt.entityType,
                            false, null, List.of(), preservedPending));
        });
        ctx.get().setPacketHandled(true);
    }
}
