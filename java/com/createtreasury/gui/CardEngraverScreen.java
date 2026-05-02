package com.createtreasury.gui;

import com.createtreasury.economy.CardUtils;
import com.createtreasury.network.EngraveCardPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CardEngraverScreen extends Screen {

    private static final int W = TreasuryScreenBase.CARD_GUI_W;
    private static final int H = TreasuryScreenBase.CARD_GUI_H;

    private final String bankName;
    private EditBox      usernameField;
    private String       errorMessage = "";

    public CardEngraverScreen(ItemStack card) {
        super(Component.translatable("createtreasury.screen.card_engraver"));
        this.bankName = CardUtils.getIssuedBy(card);
    }

    @Override
    protected void init() {
        int x = (width  - W) / 2;
        int y = (height - H) / 2;

        clearWidgets();

        usernameField = new EditBox(font, x + 21, y + 91, 113, 12,
                Component.literal("Username"));
        usernameField.setHint(Component.literal("Player username"));
        usernameField.setMaxLength(16);
        usernameField.setFilter(s -> s.matches("[a-zA-Z0-9_]{0,16}"));
        usernameField.setBordered(false);
        addRenderableWidget(usernameField);
        setInitialFocus(usernameField);

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 19, y + 120, 80, 18,
                Component.translatable("createtreasury.screen.engrave"),
                this::onEngrave, TreasuryScreenBase.CARD_TEXTURE));

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 121, y + 120, 80, 18,
                Component.translatable("gui.cancel"),
                this::onClose, TreasuryScreenBase.CARD_TEXTURE));
    }

    private void onEngrave() {
        String username = usernameField.getValue().trim();
        if (username.isEmpty()) {
            errorMessage = "Please enter a player username.";
            return;
        }
        TreasuryNetwork.CHANNEL.sendToServer(new EngraveCardPacket(username));
        onClose();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx);

        int x = (width  - W) / 2;
        int y = (height - H) / 2;

        gfx.blit(TreasuryScreenBase.CARD_TEXTURE, x, y, 0, 0, W, H, 256, 256);

        gfx.drawString(font, "Card Engraver", x + 19, y + 3, 0x3d2a00, false);
        gfx.drawString(font, "Bank: " + (bankName != null ? bankName : "?"), x + 19, y + 41, 0xc8b090, false);
        gfx.drawString(font, "Engrave for:", x + 19, y + 71, 0xc8b090, false);
        TreasuryScreenBase.blitCardInputField(gfx, x + 19, y + 86, 117, 18);

        if (!errorMessage.isEmpty()) {
            gfx.drawString(font, errorMessage, x + 19, y + 109, 0xff5555, false);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && usernameField != null && usernameField.isFocused()) {
            onEngrave();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
