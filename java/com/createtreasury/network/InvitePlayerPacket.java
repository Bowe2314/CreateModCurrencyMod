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

public class InvitePlayerPacket {

    private final String targetName;
    private final String entityType; // "Company" or "Bank"

    public InvitePlayerPacket(String targetName, String entityType) {
        this.targetName = targetName;
        this.entityType = entityType;
    }

    public static void encode(InvitePlayerPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.targetName);
        buf.writeUtf(pkt.entityType);
    }

    public static InvitePlayerPacket decode(FriendlyByteBuf buf) {
        return new InvitePlayerPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(InvitePlayerPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            CompanyManager mgr     = CompanyManager.get(sender.getServer());
            Company        company = mgr.getEntityByPlayer(sender.getUUID(), pkt.entityType);

            if (company == null) {
                sender.sendSystemMessage(Component.literal("You don't belong to a " + pkt.entityType + "."));
                return;
            }
            if (!company.getOwnerUUID().equals(sender.getUUID())) {
                sender.sendSystemMessage(Component.literal("Only the " + pkt.entityType + " owner can invite players."));
                return;
            }

            ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(pkt.targetName);
            if (target == null) {
                sender.sendSystemMessage(Component.literal("Player \"" + pkt.targetName + "\" is not online."));
                return;
            }
            if (target.getUUID().equals(sender.getUUID())) {
                sender.sendSystemMessage(Component.literal("You can't invite yourself."));
                return;
            }

            boolean ok = mgr.invitePlayer(company.getId(), target.getUUID());
            if (!ok) {
                sender.sendSystemMessage(Component.literal(pkt.targetName
                        + " already has a pending invite or is already in a " + pkt.entityType + "."));
                return;
            }

            sender.sendSystemMessage(Component.literal("Invite sent to " + pkt.targetName + "."));

            // Notify the invited player
            target.sendSystemMessage(Component.literal(
                    sender.getName().getString() + " invited you to join \"" + company.getName()
                    + "\" (" + pkt.entityType + "). Open a Company Terminal to accept or decline."));

            // Sync ONLY the pending invite to the target type=null so company/bank slots untouched
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> target),
                    new SyncCompanyPacket(null, null, null, null, false, null, List.of(), company.getName())
            );

            // Sync updated member list back to the owner
            List<String> memberNames = resolveNames(sender, company);
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new SyncCompanyPacket(company.getName(), company.getTagline(), company.getIndustry(),
                            company.getType(), true, sender.getName().getString(), memberNames, null)
            );
        });
        ctx.get().setPacketHandled(true);
    }

    private static List<String> resolveNames(ServerPlayer reference, Company company) {
        return company.getMembers().stream()
                .map(uuid -> {
                    ServerPlayer p = reference.getServer().getPlayerList().getPlayer(uuid);
                    return p != null ? p.getName().getString() : uuid.toString().substring(0, 8) + "…";
                })
                .collect(Collectors.toList());
    }
}
