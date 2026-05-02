package com.createtreasury.gui;

import com.createtreasury.client.ClientCompanyCache;
import com.createtreasury.config.Censor;
import com.createtreasury.network.AcceptInvitePacket;
import com.createtreasury.network.DeclineInvitePacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class JobBoardScreen extends TreasuryScreenBase {


    private final Screen parent;

    // Placeholder job list replaced by server-synced data when job system is built
    private static final List<String> PENDING_JOBS = List.of();

    public JobBoardScreen(Screen parent) {
        super(Component.literal("Job Board"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        if (ClientCompanyCache.hasPendingInvite()) {
            int cx  = leftPos + GUI_WIDTH / 2;
            int btnW = 90;
            addRenderableWidget(new TreasuryButton(
                    cx - btnW - 3, topPos + 110, btnW, 18,
                    Component.literal(Censor.apply("Accept")),
                    () -> {
                        TreasuryNetwork.CHANNEL.sendToServer(new AcceptInvitePacket());
                        Minecraft.getInstance().setScreen(parent);
                    }
            ));
            addRenderableWidget(new TreasuryButton(
                    cx + 3, topPos + 110, btnW, 18,
                    Component.literal(Censor.apply("Decline")),
                    () -> {
                        TreasuryNetwork.CHANNEL.sendToServer(new DeclineInvitePacket());
                        Minecraft.getInstance().setScreen(parent);
                    }
            ));
        }

        addRenderableWidget(new TreasuryButton(
                leftPos + (GUI_WIDTH - 90) / 2, topPos + 168, 90, 18,
                Component.literal(Censor.apply("Back")),
                () -> Minecraft.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderTreasuryBackground(graphics);
        renderStandardHeader(graphics);

        int cx = leftPos + GUI_WIDTH / 2;

        // Title moved down 3px from default 36 → 39
        graphics.drawCenteredString(font, Censor.apply("Job Board"), cx, topPos + 39, 0xffe0c070);

        // Pending invite section
        if (ClientCompanyCache.hasPendingInvite()) {
            // Alert icon (Create's icons.png row 2, icon 5)
            int iconX = leftPos + 18;
            int iconY = topPos + 54;
            graphics.blit(TreasuryScreenBase.CREATE_ICONS, iconX, iconY,
                    TreasuryScreenBase.ALERT_ICON_U, TreasuryScreenBase.ALERT_ICON_V,
                    TreasuryScreenBase.ALERT_ICON_SIZE, TreasuryScreenBase.ALERT_ICON_SIZE, 256, 256);

            graphics.drawString(font,
                    Censor.apply("Invite from: \"" + ClientCompanyCache.pendingInviteFrom + "\""),
                    leftPos + 18 + TreasuryScreenBase.ALERT_ICON_SIZE + 4, iconY + 4, 0xffe0c070, false);

            graphics.drawCenteredString(font, Censor.apply("Would you like to join?"),
                    cx, topPos + 95, 0xc8b090);
        }

        // Job listings only show "no jobs" when there is room (no invite taking up the space)
        boolean hasInvite = ClientCompanyCache.hasPendingInvite();
        int jobsY = hasInvite ? topPos + 134 : topPos + 56;
        if (PENDING_JOBS.isEmpty()) {
            if (!hasInvite) {
                graphics.drawCenteredString(font, Censor.apply("No jobs available."), cx, jobsY + 10, 0xc8b090);
            }
        } else {
            for (int i = 0; i < Math.min(PENDING_JOBS.size(), 5); i++) {
                graphics.drawString(font, Censor.apply("• " + PENDING_JOBS.get(i)),
                        leftPos + 20, jobsY + i * 12, 0xffe0c070, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
