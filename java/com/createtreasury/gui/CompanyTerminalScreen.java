package com.createtreasury.gui;

import com.createtreasury.client.ClientCompanyCache;
import com.createtreasury.config.Censor;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CompanyTerminalScreen extends AbstractSimiContainerScreen<CompanyTerminalMenu> {

    // Checkmark button bottom-right of the texture icon row
    private static final int CHECK_X = 235;
    private static final int CHECK_Y = 196;
    private static final int CHECK_W = 18;
    private static final int CHECK_H = 18;

    // State tracked in renderForeground to trigger rebuildWidgets when needed
    private boolean lastHasCompany;
    private boolean lastHasBank;

    private TreasuryScreenBase.TreasuryButton companyBtn;
    private TreasuryScreenBase.TreasuryButton bankBtn;

    public CompanyTerminalScreen(CompanyTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth  = TreasuryScreenBase.GUI_WIDTH;
        imageHeight = TreasuryScreenBase.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        // Checkmark positioned over the checkmark icon in the texture
        addRenderableWidget(new IconButton(
                leftPos + CHECK_X, topPos + CHECK_Y, CHECK_W, CHECK_H,
                () -> {}  // TODO: confirm action
        ));

        int btnX = leftPos + (TreasuryScreenBase.GUI_WIDTH - 110) / 2;

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                btnX, topPos + 50, 110, 18,
                Component.literal(Censor.apply("Job Board")),
                () -> Minecraft.getInstance().setScreen(new JobBoardScreen(this))
        ));

        // Company button
        companyBtn = new TreasuryScreenBase.TreasuryButton(
                btnX, topPos + 74, 110, 18,
                Component.literal(companyLabel()),
                () -> openManage("Company")
        );
        addRenderableWidget(companyBtn);

        // Bank button
        bankBtn = new TreasuryScreenBase.TreasuryButton(
                btnX, topPos + 98, 110, 18,
                Component.literal(bankLabel()),
                () -> openManage("Bank")
        );
        addRenderableWidget(bankBtn);

        lastHasCompany = ClientCompanyCache.hasCompany();
        lastHasBank    = ClientCompanyCache.hasBank();
    }

    private String companyLabel() {
        return Censor.apply(ClientCompanyCache.hasCompany() ? "Manage Company" : "Create Company");
    }

    private String bankLabel() {
        return Censor.apply(ClientCompanyCache.hasBank() ? "Manage Bank" : "Create Bank");
    }

    private void openManage(String type) {
        if (ClientCompanyCache.hasEntity(type) || ClientCompanyCache.hasPendingInvite()) {
            Minecraft.getInstance().setScreen(new ManageCompanyScreen(this, type));
        } else {
            // Type is already known from which button was clicked go straight to creation
            Minecraft.getInstance().setScreen(new CreateCompanyScreen(this, type));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TreasuryScreenBase.TEXTURE, leftPos, topPos, TreasuryScreenBase.GUI_OFFSET, 0,
                TreasuryScreenBase.GUI_WIDTH, TreasuryScreenBase.GUI_HEIGHT, 256, 256);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hasCompany = ClientCompanyCache.hasCompany();
        boolean hasBank    = ClientCompanyCache.hasBank();

        // Keep button labels live
        if (companyBtn != null) companyBtn.setMessage(Component.literal(companyLabel()));
        if (bankBtn    != null) bankBtn.setMessage(Component.literal(bankLabel()));

        // Rebuild on state change
        if (hasCompany != lastHasCompany || hasBank != lastHasBank) {
            lastHasCompany = hasCompany;
            lastHasBank    = hasBank;
            rebuildWidgets();
            return;
        }

        // Title
        graphics.drawString(font, Censor.apply("Company Terminal"), leftPos + 4, topPos + 4, 0x3d2a00, false);

        // Employment line show combined status
        String employment;
        if (hasCompany && hasBank) {
            employment = Censor.apply("Employment: " + ClientCompanyCache.companyName
                    + " & " + ClientCompanyCache.bankName);
        } else if (hasCompany) {
            employment = Censor.apply("Employment: " + ClientCompanyCache.companyName);
        } else if (hasBank) {
            employment = Censor.apply("Employment: " + ClientCompanyCache.bankName + " [Bank]");
        } else {
            employment = Censor.apply("Employment: Unemployed");
        }
        graphics.drawString(font, employment, leftPos + 7, topPos + 21, 0xc8b090, false);

        // Alert icon at top-right of Job Board button when there's a pending invite
        if (ClientCompanyCache.hasPendingInvite()) {
            int btnX  = leftPos + (TreasuryScreenBase.GUI_WIDTH - 110) / 2;
            int iconW = TreasuryScreenBase.ALERT_ICON_SIZE;
            TreasuryScreenBase.blitStretched(graphics, TreasuryScreenBase.CREATE_ICONS,
                    btnX + 110 - iconW / 2, topPos + 50 - iconW / 2,
                    iconW, iconW,
                    TreasuryScreenBase.ALERT_ICON_U, TreasuryScreenBase.ALERT_ICON_V,
                    TreasuryScreenBase.ALERT_ICON_SIZE, TreasuryScreenBase.ALERT_ICON_SIZE);
        }
    }

    // Invisible button that just renders a hover highlight over a texture icon
    private static class IconButton extends AbstractButton {

        private final Runnable action;

        public IconButton(int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.empty());
            this.action = action;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (isHovered()) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height,
                        TreasuryScreenBase.HOVER_COLOR);
            }
        }

        @Override
        public void onPress() { action.run(); }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
}
