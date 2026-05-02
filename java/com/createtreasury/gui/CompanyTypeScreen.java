package com.createtreasury.gui;

import com.createtreasury.config.Censor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CompanyTypeScreen extends TreasuryScreenBase {

    private final Screen parent;

    public CompanyTypeScreen(Screen parent) {
        super(Component.literal("Select Type"));
        this.parent = parent;
    }

    public Screen getParent() { return parent; }

    @Override
    protected void init() {
        super.init();

        int btnX = leftPos + (GUI_WIDTH - 110) / 2;

        addRenderableWidget(new TreasuryButton(
                btnX, topPos + 80, 110, 18,
                Component.literal(Censor.apply("Company")),
                () -> Minecraft.getInstance().setScreen(new CreateCompanyScreen(this, "Company"))
        ));

        addRenderableWidget(new TreasuryButton(
                btnX, topPos + 104, 110, 18,
                Component.literal(Censor.apply("Bank")),
                () -> Minecraft.getInstance().setScreen(new CreateCompanyScreen(this, "Bank"))
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderTreasuryBackground(graphics);
        renderStandardHeader(graphics);
        graphics.drawCenteredString(font, Censor.apply("What would you like to manage?"),
                leftPos + GUI_WIDTH / 2, topPos + 55, 0xc8b090);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
