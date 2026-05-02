package com.createtreasury.gui;

import com.createtreasury.config.Censor;
import com.createtreasury.network.CreateCompanyPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CreateCompanyScreen extends TreasuryScreenBase {

    private static final String[] INDUSTRIES = { "Manufacturing", "Trading", "Mining", "Agriculture" };

    private final Screen parent;
    private final String type;   // "Company" or "Bank"

    private EditBox nameField;
    private EditBox taglineField;
    private String selectedIndustry = INDUSTRIES[0];
    private final List<ToggleTreasuryButton> industryBtns = new ArrayList<>();

    public CreateCompanyScreen(Screen parent, String type) {
        super(Component.literal("Create " + type));
        this.parent = parent;
        this.type   = type;
    }

    @Override
    protected void init() {
        super.init();
        industryBtns.clear();

        // ---- text fields ----
        nameField = new EditBox(font, leftPos + 26, topPos + 53, 174, 12,
                Component.literal("Company Name"));
        nameField.setMaxLength(27);
        nameField.setHint(Component.literal("Enter name..."));
        nameField.setBordered(false);
        addRenderableWidget(nameField);

        taglineField = new EditBox(font, leftPos + 26, topPos + 86, 174, 12,
                Component.literal("Tagline"));
        taglineField.setMaxLength(64);
        taglineField.setHint(Component.literal("Short motto..."));
        taglineField.setBordered(false);
        addRenderableWidget(taglineField);

        // ---- industry radio buttons (2 × 2 grid) ----
        int indW = 95, indH = 18, gap = 6;
        int col1 = leftPos + (GUI_WIDTH - indW * 2 - gap) / 2;
        int col2 = col1 + indW + gap;
        int row1 = topPos + 115;
        int row2 = topPos + 139;

        addIndustryBtn(INDUSTRIES[0], col1, row1, indW, indH);
        addIndustryBtn(INDUSTRIES[1], col2, row1, indW, indH);
        addIndustryBtn(INDUSTRIES[2], col1, row2, indW, indH);
        addIndustryBtn(INDUSTRIES[3], col2, row2, indW, indH);

        // ---- confirm ----
        addRenderableWidget(new TreasuryButton(
                leftPos + (GUI_WIDTH - 110) / 2, topPos + 168, 110, 18,
                Component.literal("Create"),
                this::onConfirm
        ));
    }

    private void addIndustryBtn(String industry, int x, int y, int w, int h) {
        ToggleTreasuryButton btn = new ToggleTreasuryButton(x, y, w, h,
                Component.literal(Censor.apply(industry)), () -> {
            selectedIndustry = industry;
            industryBtns.forEach(b -> b.setSelected(b.getMessage().getString().equals(industry)));
        });
        btn.setSelected(industry.equals(selectedIndustry));
        industryBtns.add(btn);
        addRenderableWidget(btn);
    }

    private void onConfirm() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) return;
        if (name.length() > 27) name = name.substring(0, 27);
        TreasuryNetwork.CHANNEL.sendToServer(
                new CreateCompanyPacket(name, taglineField.getValue().trim(), selectedIndustry, type));
        // Go back to the terminal screen (skip the type-selection screen)
        Screen destination = (parent instanceof CompanyTypeScreen cts) ? cts.getParent() : parent;
        Minecraft.getInstance().setScreen(destination);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderTreasuryBackground(graphics);
        renderStandardHeader(graphics);

        graphics.drawString(font, Censor.apply("Company Name"), leftPos + 24, topPos + 40, 0xc8b090, false);
        blitInputField(graphics, leftPos + 24, topPos + 48, 178, 18);

        graphics.drawString(font, Censor.apply("Tagline"),  leftPos + 24, topPos + 73, 0xc8b090, false);
        blitInputField(graphics, leftPos + 24, topPos + 81, 178, 18);

        graphics.drawString(font, Censor.apply("Industry"), leftPos + 28, topPos + 105, 0xc8b090, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
