package com.createtreasury.network;

import com.createtreasury.client.ClientCompanyCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Sent server → client when the player opens the Company Terminal. */
public class SyncCompanyPacket {

    @Nullable private final String       companyName;
    @Nullable private final String       tagline;
    @Nullable private final String       industry;
    @Nullable private final String       type;
    private final           boolean      isOwner;
    @Nullable private final String       ownerName;
    private final           List<String> memberNames;
    @Nullable private final String       pendingInviteFrom;

    public SyncCompanyPacket(@Nullable String companyName, @Nullable String tagline,
                             @Nullable String industry, @Nullable String type,
                             boolean isOwner, @Nullable String ownerName,
                             List<String> memberNames, @Nullable String pendingInviteFrom) {
        this.companyName       = companyName;
        this.tagline           = tagline;
        this.industry          = industry;
        this.type              = type;
        this.isOwner           = isOwner;
        this.ownerName         = ownerName;
        this.memberNames       = memberNames;
        this.pendingInviteFrom = pendingInviteFrom;
    }

    public static void encode(SyncCompanyPacket pkt, FriendlyByteBuf buf) {
        writeNullable(buf, pkt.companyName);
        writeNullable(buf, pkt.tagline);
        writeNullable(buf, pkt.industry);
        writeNullable(buf, pkt.type);
        buf.writeBoolean(pkt.isOwner);
        writeNullable(buf, pkt.ownerName);
        buf.writeInt(pkt.memberNames.size());
        for (String s : pkt.memberNames) buf.writeUtf(s);
        writeNullable(buf, pkt.pendingInviteFrom);
    }

    public static SyncCompanyPacket decode(FriendlyByteBuf buf) {
        String companyName   = readNullable(buf);
        String tagline       = readNullable(buf);
        String industry      = readNullable(buf);
        String type          = readNullable(buf);
        boolean isOwner      = buf.readBoolean();
        String ownerName     = readNullable(buf);
        int memberCount      = buf.readInt();
        List<String> members = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) members.add(buf.readUtf());
        String pendingFrom   = readNullable(buf);
        return new SyncCompanyPacket(companyName, tagline, industry, type, isOwner, ownerName, members, pendingFrom);
    }

    public static void handle(SyncCompanyPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                ClientCompanyCache.update(pkt.companyName, pkt.tagline, pkt.industry,
                        pkt.type, pkt.isOwner, pkt.ownerName, pkt.memberNames, pkt.pendingInviteFrom)
        );
        ctx.get().setPacketHandled(true);
    }

    private static void writeNullable(FriendlyByteBuf buf, @Nullable String s) {
        buf.writeBoolean(s != null);
        if (s != null) buf.writeUtf(s);
    }

    @Nullable
    private static String readNullable(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUtf() : null;
    }
}
