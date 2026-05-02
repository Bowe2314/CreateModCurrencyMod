package com.createtreasury.gui;

import com.createtreasury.client.ClientCompanyCache;
import com.createtreasury.config.Censor;
import com.createtreasury.item.CompanyLinkerItem;
import com.createtreasury.network.SetLinkerSelectionPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Standalone screen opened when the player right-clicks a Company Linker.
 * Lets the player choose which Company or Bank to stamp onto minted coins.
 */
public class CompanyLinkerScreen extends TreasuryScreenBase {

    // Layout within the GUI panel
    private static final int TITLE_Y       = 4;
    private static final int SUBTITLE_Y    = 38;
    private static final int COMPANY_ROW_Y = 60;
    private static final int BANK_ROW_Y    = 90;
    private static final int SELECTED_Y    = 126;
    private static final int BTN_W         = 80;
    private static final int BTN_H         = 18;
    private static final int CLOSE_Y       = 158;
    private static final int CLOSE_W       = 60;

    /** Current selection shown in the screen (mirrors item NBT). */
    private String selectedType = null;
    private String selectedName = null;

    public CompanyLinkerScreen() {
        super(Component.literal("Company Linker"));
        // Read current selection from held item
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack held = mc.player.getMainHandItem();
            selectedType = CompanyLinkerItem.getSelectedType(held);
            selectedName = CompanyLinkerItem.getSelectedName(held);
        }
    }

    @Override
    protected void init() {
        super.init();

        int cx = leftPos + GUI_WIDTH / 2;

        // ── Company row ──────────────────────────────────────────────────────
        if (ClientCompanyCache.hasCompany()) {
            final String cName = ClientCompanyCache.companyName;
            addRenderableWidget(new TreasuryButton(
                    cx - BTN_W / 2, topPos + COMPANY_ROW_Y, BTN_W, BTN_H,
                    Component.literal(Censor.apply("Select Company")),
                    () -> selectEntity("Company", cName)
            ));
        }

        // ── Bank row ─────────────────────────────────────────────────────────
        if (ClientCompanyCache.hasBank()) {
            final String bName = ClientCompanyCache.bankName;
            addRenderableWidget(new TreasuryButton(
                    cx - BTN_W / 2, topPos + BANK_ROW_Y, BTN_W, BTN_H,
                    Component.literal(Censor.apply("Select Bank")),
                    () -> selectEntity("Bank", bName)
            ));
        }

        // ── Clear button ─────────────────────────────────────────────────────
        addRenderableWidget(new TreasuryButton(
                cx - CLOSE_W / 2, topPos + CLOSE_Y, CLOSE_W, BTN_H,
                Component.literal("Close"),
                () -> onClose()
        ));
    }

    private void selectEntity(String type, String name) {
        selectedType = type;
        selectedName = name;
        TreasuryNetwork.CHANNEL.sendToServer(new SetLinkerSelectionPacket(type, name));
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderTreasuryBackground(graphics);

        int cx = leftPos + GUI_WIDTH / 2;

        // Title
        graphics.drawCenteredString(font, Censor.apply("Company Linker"),
                cx, topPos + TITLE_Y + 16, 0x3d2a00);

        // Subtitle
        graphics.drawCenteredString(font, "Select an entity to stamp onto coins",
                cx, topPos + SUBTITLE_Y, 0xc8b090);

        // Company row
        if (ClientCompanyCache.hasCompany()) {
            graphics.drawCenteredString(font,
                    Censor.apply("Company: " + ClientCompanyCache.companyName),
                    cx, topPos + COMPANY_ROW_Y - 12, 0xffe0c070);
        } else {
            graphics.drawCenteredString(font, "No Company", cx, topPos + COMPANY_ROW_Y - 12, 0x909090);
            graphics.drawCenteredString(font, "(join or create a Company first)",
                    cx, topPos + COMPANY_ROW_Y + 5, 0x707070);
        }

        // Bank row
        if (ClientCompanyCache.hasBank()) {
            graphics.drawCenteredString(font,
                    Censor.apply("Bank: " + ClientCompanyCache.bankName),
                    cx, topPos + BANK_ROW_Y - 12, 0xffe0c070);
        } else {
            graphics.drawCenteredString(font, "No Bank", cx, topPos + BANK_ROW_Y - 12, 0x909090);
            graphics.drawCenteredString(font, "(join or create a Bank first)",
                    cx, topPos + BANK_ROW_Y + 5, 0x707070);
        }

        // Current selection
        String selLine;
        if (selectedName != null && selectedType != null) {
            selLine = "§fLinked: " + Censor.apply(selectedName) + " [" + selectedType + "]";
        } else {
            selLine = "§8Linked: None";
        }
        graphics.drawCenteredString(font, selLine, cx, topPos + SELECTED_Y, 0xc8b090);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
