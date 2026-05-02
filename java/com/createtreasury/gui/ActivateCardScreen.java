package com.createtreasury.gui;

import com.createtreasury.config.TreasuryServerConfig;
import com.createtreasury.economy.CardUtils;
import com.createtreasury.network.ActivateCardPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ActivateCardScreen extends Screen {

    private static final int W = TreasuryScreenBase.CARD_GUI_W;
    private static final int H = TreasuryScreenBase.CARD_GUI_H;

    private final InteractionHand hand;
    private final ItemStack        card;
    private final String           bankName;

    private EditBox pinField;

    public ActivateCardScreen(InteractionHand hand, ItemStack card) {
        super(Component.translatable("createtreasury.screen.activate_card"));
        this.hand     = hand;
        this.card     = card;
        this.bankName = CardUtils.getIssuedBy(card);
    }

    @Override
    protected void init() {
        int x = (width  - W) / 2;
        int y = (height - H) / 2;
        clearWidgets();

        boolean pinsEnabled = TreasuryServerConfig.SERVER.enablePins.get();
        if (pinsEnabled) {
            int pinMax = TreasuryServerConfig.SERVER.pinMaxLength.get();
            int pinMin = TreasuryServerConfig.SERVER.pinMinLength.get();
            boolean required = TreasuryServerConfig.SERVER.requirePinOnActivation.get();

            pinField = new EditBox(font, x + 21, y + 91, 109, 12,
                    Component.translatable("createtreasury.screen.pin"));
            pinField.setHint(Component.literal(
                    "PIN (" + (required ? "" : "optional, ") + pinMin + "-" + pinMax + " digits)"));
            pinField.setMaxLength(pinMax);
            pinField.setFilter(s -> s.matches("\\d{0," + pinMax + "}"));
            pinField.setBordered(false);
            addRenderableWidget(pinField);
        }

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 19, y + 120, 80, 18,
                Component.translatable("createtreasury.screen.activate"),
                this::onActivate, TreasuryScreenBase.CARD_TEXTURE));

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 121, y + 120, 80, 18,
                Component.translatable("gui.cancel"),
                this::onClose, TreasuryScreenBase.CARD_TEXTURE));
    }

    private void onActivate() {
        boolean pinsEnabled = TreasuryServerConfig.SERVER.enablePins.get();
        String pin = (pinsEnabled && pinField != null) ? pinField.getValue().trim() : "";

        if (pinsEnabled && !pin.isEmpty()) {
            int pinMin = TreasuryServerConfig.SERVER.pinMinLength.get();
            int pinMax = TreasuryServerConfig.SERVER.pinMaxLength.get();
            if (pin.length() < pinMin || pin.length() > pinMax) return;
        }

        TreasuryNetwork.CHANNEL.sendToServer(new ActivateCardPacket(hand, pin));
        onClose();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx);

        int x = (width  - W) / 2;
        int y = (height - H) / 2;

        gfx.blit(TreasuryScreenBase.CARD_TEXTURE, x, y, 0, 0, W, H, 256, 256);

        gfx.drawString(font, "Activate Debit Card", x + 19, y + 3, 0x3d2a00, false);
        gfx.drawString(font, "Issued by: " + (bankName != null ? bankName : "?"),
                x + 19, y + 41, 0xc8b090, false);

        boolean pinsEnabled = TreasuryServerConfig.SERVER.enablePins.get();
        if (pinsEnabled) {
            boolean required = TreasuryServerConfig.SERVER.requirePinOnActivation.get();
            gfx.drawString(font, "PIN" + (required ? ":" : " (optional):"),
                    x + 19, y + 71, 0xc8b090, false);
            TreasuryScreenBase.blitCardInputField(gfx, x + 19, y + 86, 113, 18);
        }

        gfx.drawString(font, "Activating creates a bank account in your name.",
                x + 33, y + 144, 0x888888, false);

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
