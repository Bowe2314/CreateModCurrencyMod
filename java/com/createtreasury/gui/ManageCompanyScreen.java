package com.createtreasury.gui;

import com.createtreasury.client.ClientCompanyCache;
import com.createtreasury.config.Censor;
import com.createtreasury.network.AcceptInvitePacket;
import com.createtreasury.network.DeclineInvitePacket;
import com.createtreasury.network.KickMemberPacket;
import com.createtreasury.network.LeaveCompanyPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ManageCompanyScreen extends TreasuryScreenBase {

    // Row height for member list entries tall enough for a 12px kick button
    private static final int ROW_H         = 12;
    private static final int MEMBERS_TOP_Y = 88; // relative to topPos

    private final Screen parent;
    private final String viewType; // "Company" or "Bank"

    // Cached state for change detection in tick()
    private int     lastMemberCount = -1;
    private boolean lastHasEntity   = false;
    private boolean lastHasPending  = false;

    public ManageCompanyScreen(Screen parent, String viewType) {
        super(Component.literal("Manage " + viewType));
        this.parent   = parent;
        this.viewType = viewType;
    }

    @Override
    public void tick() {
        super.tick();
        int     memberCount = ClientCompanyCache.getMemberNamesFor(viewType).size();
        boolean hasEntity   = ClientCompanyCache.hasEntity(viewType);
        boolean hasPending  = ClientCompanyCache.hasPendingInvite();
        if (memberCount != lastMemberCount || hasEntity != lastHasEntity || hasPending != lastHasPending) {
            lastMemberCount = memberCount;
            lastHasEntity   = hasEntity;
            lastHasPending  = hasPending;
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        super.init();
        lastMemberCount = ClientCompanyCache.getMemberNamesFor(viewType).size();
        lastHasEntity   = ClientCompanyCache.hasEntity(viewType);
        lastHasPending  = ClientCompanyCache.hasPendingInvite();

        int centerX = leftPos + GUI_WIDTH / 2;
        int btnW    = 110;

        if (ClientCompanyCache.hasPendingInvite()) {
            addRenderableWidget(new TreasuryButton(
                    centerX - btnW / 2, topPos + 144, btnW, 18,
                    Component.literal(Censor.apply("Accept")),
                    () -> {
                        TreasuryNetwork.CHANNEL.sendToServer(new AcceptInvitePacket());
                        Minecraft.getInstance().setScreen(parent);
                    }
            ));
            addRenderableWidget(new TreasuryButton(
                    centerX - btnW / 2, topPos + 166, btnW, 18,
                    Component.literal(Censor.apply("Decline")),
                    () -> {
                        TreasuryNetwork.CHANNEL.sendToServer(new DeclineInvitePacket());
                        Minecraft.getInstance().setScreen(parent);
                    }
            ));

        } else if (ClientCompanyCache.hasEntity(viewType)) {
            // Kick buttons only the owner sees them
            if (ClientCompanyCache.isOwnerOf(viewType)) {
                addKickButtons();
            }

            addRenderableWidget(new TreasuryButton(
                    centerX - btnW / 2, topPos + 140, btnW, 18,
                    Component.literal(Censor.apply("Invite Player")),
                    () -> Minecraft.getInstance().setScreen(new InvitePlayerScreen(this, viewType))
            ));

            boolean isOwner   = ClientCompanyCache.isOwnerOf(viewType);
            String leaveLabel = Censor.apply(isOwner ? "Disband" : "Leave");
            addRenderableWidget(new TreasuryButton(
                    centerX - btnW / 2, topPos + 164, btnW, 18,
                    Component.literal(leaveLabel),
                    () -> {
                        if (isOwner) {
                            String name = ClientCompanyCache.getName(viewType);
                            Minecraft.getInstance().setScreen(new TreasuryConfirmScreen(
                                    this,
                                    "Disband " + viewType + "?",
                                    name != null ? name : "",
                                    () -> {
                                        TreasuryNetwork.CHANNEL.sendToServer(new LeaveCompanyPacket(viewType));
                                        Minecraft.getInstance().setScreen(parent);
                                    }
                            ));
                        } else {
                            TreasuryNetwork.CHANNEL.sendToServer(new LeaveCompanyPacket(viewType));
                            Minecraft.getInstance().setScreen(parent);
                        }
                    }
            ));
        }
    }

    private void addKickButtons() {
        String ownerName = ClientCompanyCache.getOwnerNameFor(viewType);
        List<String> others = buildOthersList(ownerName);
        int showCount = others.size() > 4 ? 3 : others.size();

        for (int i = 0; i < showCount; i++) {
            final String memberName = others.get(i);
            // +1 to account for the owner row above
            int kickY = topPos + MEMBERS_TOP_Y + (i + 1) * ROW_H;
            addRenderableWidget(new TreasuryButton(
                    leftPos + 192, kickY, 26, ROW_H,
                    Component.literal("X"),
                    () -> Minecraft.getInstance().setScreen(new TreasuryConfirmScreen(
                            this,
                            "Kick member?",
                            memberName,
                            () -> {
                                TreasuryNetwork.CHANNEL.sendToServer(
                                        new KickMemberPacket(memberName, viewType));
                                rebuildWidgets();
                            }
                    ))
            ));
        }
    }

    private List<String> buildOthersList(String ownerName) {
        List<String> others = new ArrayList<>();
        for (String m : ClientCompanyCache.getMemberNamesFor(viewType)) {
            if (!m.equals(ownerName)) others.add(m);
        }
        return others;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderTreasuryBackground(graphics);
        renderStandardHeader(graphics);

        int cx = leftPos + GUI_WIDTH / 2;

        if (ClientCompanyCache.hasPendingInvite()) {
            graphics.drawCenteredString(font, Censor.apply("Pending Invite"), cx, topPos + 38, 0xffe0c070);
            graphics.drawCenteredString(font,
                    "\"" + Censor.apply(ClientCompanyCache.pendingInviteFrom) + "\"", cx, topPos + 52, 0xc8b090);
            graphics.drawCenteredString(font, Censor.apply("Would you like to join?"), cx, topPos + 66, 0xc8b090);

        } else if (ClientCompanyCache.hasEntity(viewType)) {
            String entityName = ClientCompanyCache.getName(viewType);
            String typeTag = " [" + viewType + "]";
            graphics.drawCenteredString(font, Censor.apply(entityName + typeTag), cx, topPos + 38, 0xffe0c070);

            String tl = ClientCompanyCache.getTaglineFor(viewType);
            if (tl != null && !tl.isEmpty()) {
                graphics.drawCenteredString(font, "\"" + Censor.apply(tl) + "\"",
                        cx, topPos + 50, 0xa0907060);
            }
            String ind = ClientCompanyCache.getIndustryFor(viewType);
            if (ind != null) {
                graphics.drawCenteredString(font, Censor.apply(ind), cx, topPos + 62, 0xc8b090);
            }

            graphics.drawString(font, Censor.apply("Members:"), leftPos + 20, topPos + 76, 0xc8b090, false);

            String ownerName = ClientCompanyCache.getOwnerNameFor(viewType);
            int y = topPos + MEMBERS_TOP_Y;

            if (ownerName != null) {
                graphics.drawString(font, Censor.apply("• " + ownerName + " (Owner)"),
                        leftPos + 28, y, 0xffe0c070, false);
                y += ROW_H;
            }

            List<String> others = buildOthersList(ownerName);
            if (others.size() > 4) {
                for (int i = 0; i < 3; i++) {
                    graphics.drawString(font, Censor.apply("• " + others.get(i)),
                            leftPos + 28, y, 0xffe0c070, false);
                    y += ROW_H;
                }
                graphics.drawString(font, "  +" + (others.size() - 3) + " more",
                        leftPos + 28, y, 0x909090, false);
            } else {
                for (String m : others) {
                    graphics.drawString(font, Censor.apply("• " + m),
                            leftPos + 28, y, 0xffe0c070, false);
                    y += ROW_H;
                }
            }
        } else {
            graphics.drawCenteredString(font, Censor.apply("No " + viewType + " found."), cx, topPos + 80, 0xc8b090);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
