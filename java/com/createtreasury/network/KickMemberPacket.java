package com.createtreasury.network;

import com.createtreasury.company.Company;
import com.createtreasury.company.CompanyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class KickMemberPacket {

    private final String targetName;
    private final String entityType; // "Company" or "Bank"

    public KickMemberPacket(String targetName, String entityType) {
        this.targetName = targetName;
        this.entityType = entityType;
    }

    public static void encode(KickMemberPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.targetName);
        buf.writeUtf(pkt.entityType);
    }

    public static KickMemberPacket decode(FriendlyByteBuf buf) {
        return new KickMemberPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(KickMemberPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer owner = ctx.get().getSender();
            if (owner == null) return;

            CompanyManager mgr     = CompanyManager.get(owner.getServer());
            Company        company = mgr.getEntityByPlayer(owner.getUUID(), pkt.entityType);

            if (company == null || !company.getOwnerUUID().equals(owner.getUUID())) {
                owner.sendSystemMessage(Component.literal("You are not a " + pkt.entityType + " owner."));
                return;
            }

            // Resolve target UUID try online player first, then profile cache
            UUID targetUUID = null;
            ServerPlayer targetOnline = owner.getServer().getPlayerList().getPlayerByName(pkt.targetName);
            if (targetOnline != null) {
                targetUUID = targetOnline.getUUID();
            } else {
                Optional<com.mojang.authlib.GameProfile> profile =
                        owner.getServer().getProfileCache().get(pkt.targetName);
                if (profile.isPresent()) targetUUID = profile.get().getId();
            }

            if (targetUUID == null) {
                owner.sendSystemMessage(Component.literal("Player \"" + pkt.targetName + "\" not found."));
                return;
            }

            // Preserve the kicked player's pending invite BEFORE kicking them
            String kickedPending = mgr.getPendingInviteName(targetUUID);

            boolean ok = mgr.kickMember(owner.getUUID(), targetUUID, pkt.entityType);
            if (!ok) {
                owner.sendSystemMessage(Component.literal("Could not kick \"" + pkt.targetName + "\"."));
                return;
            }

            owner.sendSystemMessage(Component.literal("Kicked " + pkt.targetName + " from the " + pkt.entityType + "."));

            // Notify the kicked player if online clear their slot but preserve any pending invite
            if (targetOnline != null) {
                targetOnline.sendSystemMessage(
                        Component.literal("You were kicked from \"" + company.getName() + "\"."));
                TreasuryNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> targetOnline),
                        new SyncCompanyPacket(null, null, null, pkt.entityType,
                                false, null, List.of(), kickedPending));
            }

            // Build updated member list for remaining members
            List<String> memberNames = company.getMembers().stream()
                    .map(uuid -> {
                        ServerPlayer p = owner.getServer().getPlayerList().getPlayer(uuid);
                        return p != null ? p.getName().getString() : uuid.toString().substring(0, 8) + "…";
                    })
                    .collect(Collectors.toList());

            String ownerName = owner.getName().getString();

            // Sync to remaining non-owner members
            for (UUID memberId : company.getMembers()) {
                ServerPlayer member = owner.getServer().getPlayerList().getPlayer(memberId);
                if (member == null) continue;
                TreasuryNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> member),
                        new SyncCompanyPacket(company.getName(), company.getTagline(), company.getIndustry(),
                                company.getType(), false, ownerName, memberNames,
                                mgr.getPendingInviteName(memberId)));
            }

            // Sync to the owner explicitly (owner is not in company.getMembers())
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> owner),
                    new SyncCompanyPacket(company.getName(), company.getTagline(), company.getIndustry(),
                            company.getType(), true, ownerName, memberNames, null));
        });
        ctx.get().setPacketHandled(true);
    }
}
