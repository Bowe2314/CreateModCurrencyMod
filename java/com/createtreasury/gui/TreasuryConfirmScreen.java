package com.createtreasury.gui;

import com.createtreasury.config.Censor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TreasuryConfirmScreen extends TreasuryScreenBase {

    private final Screen   parent;
    private final String   heading;
    private final String   detail;
    private final Runnable onConfirm;

    /**
     * @param heading  Bold question line, e.g. "Disband company?"
     * @param detail   Secondary detail line, e.g. the company or player name
     * @param onConfirm Runs when the player clicks Confirm
     */
    public TreasuryConfirmScreen(Screen parent, String heading, String detail, Runnable onConfirm) {
        super(Component.literal(heading));
        this.parent    = parent;
        this.heading   = heading;
        this.detail    = detail;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();

        int cx   = leftPos + GUI_WIDTH / 2;
        int btnW = 90;

        addRenderableWidget(new TreasuryButton(
                cx - btnW - 4, topPos + 130, btnW, 18,
                Component.literal(Censor.apply("Confirm")),
                () -> {
                    onConfirm.run();
                    Minecraft.getInstance().setScreen(parent);
                }
        ));
        addRenderableWidget(new TreasuryButton(
                cx + 4, topPos + 130, btnW, 18,
                Component.literal(Censor.apply("Cancel")),
                () -> Minecraft.getInstance().setScreen(parent)
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderTreasuryBackground(graphics);
        renderStandardHeader(graphics);

        int cx = leftPos + GUI_WIDTH / 2;
        graphics.drawCenteredString(font, Censor.apply(heading), cx, topPos + 75, 0xffe0c070);
        if (!detail.isEmpty()) {
            graphics.drawCenteredString(font, Censor.apply(detail), cx, topPos + 93, 0xc8b090);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
