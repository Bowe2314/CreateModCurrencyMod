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

public class CreateCompanyPacket {

    private final String name;
    private final String tagline;
    private final String industry;
    private final String type;

    public CreateCompanyPacket(String name, String tagline, String industry, String type) {
        this.name     = name;
        this.tagline  = tagline;
        this.industry = industry;
        this.type     = type;
    }

    public static void encode(CreateCompanyPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.name);
        buf.writeUtf(pkt.tagline);
        buf.writeUtf(pkt.industry);
        buf.writeUtf(pkt.type);
    }

    public static CreateCompanyPacket decode(FriendlyByteBuf buf) {
        return new CreateCompanyPacket(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(CreateCompanyPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            CompanyManager mgr = CompanyManager.get(player.getServer());
            if (pkt.name.isBlank() || pkt.name.length() > 27) {
                player.sendSystemMessage(Component.literal("Invalid company name (1–27 characters)."));
                return;
            }
            if (!mgr.canJoinType(player.getUUID(), pkt.type)) {
                player.sendSystemMessage(Component.literal("You already own or belong to a " + pkt.type + "."));
                return;
            }

            Company c = mgr.createCompany(pkt.name, pkt.tagline, pkt.industry, pkt.type, player.getUUID());
            player.sendSystemMessage(Component.literal("Company \"" + c.getName() + "\" created!"));

            // Sync updated state back to the player
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncCompanyPacket(c.getName(), c.getTagline(), c.getIndustry(),
                            c.getType(), true, player.getName().getString(), List.of(), null)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
