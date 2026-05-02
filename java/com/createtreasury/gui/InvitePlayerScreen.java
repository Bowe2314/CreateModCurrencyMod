package com.createtreasury.gui;

import com.createtreasury.config.Censor;
import com.createtreasury.network.InvitePlayerPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class InvitePlayerScreen extends TreasuryScreenBase {

    private final Screen parent;
    private final String entityType; // "Company" or "Bank"
    private EditBox nameField;

    public InvitePlayerScreen(Screen parent, String entityType) {
        super(Component.literal("Invite Player"));
        this.parent     = parent;
        this.entityType = entityType;
    }

    @Override
    protected void init() {
        super.init();

        nameField = new EditBox(font, leftPos + 30, topPos + 83, 166, 12,
                Component.literal("Player Name"));
        nameField.setMaxLength(40);
        nameField.setHint(Component.literal("Enter username..."));
        nameField.setBordered(false);
        addRenderableWidget(nameField);

        addRenderableWidget(new TreasuryButton(
                leftPos + (GUI_WIDTH - 110) / 2, topPos + 110, 110, 18,
                Component.literal(Censor.apply("Send Invite")),
                this::sendInvite
        ));
    }

    private void sendInvite() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) return;
        TreasuryNetwork.CHANNEL.sendToServer(new InvitePlayerPacket(name, entityType));
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderTreasuryBackground(graphics);
        renderStandardHeader(graphics);
        graphics.drawString(font, Censor.apply("Player name:"), leftPos + 28, topPos + 70, 0xc8b090, false);
        blitInputField(graphics, leftPos + 28, topPos + 78, 170, 18);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
