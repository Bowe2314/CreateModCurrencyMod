package com.createtreasury.gui;

import com.createtreasury.client.ClientCompanyCache;
import com.createtreasury.config.Censor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class TreasuryScreenBase extends Screen {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation("createtreasury", "textures/gui/company_terminal_gui.png");

    public static final ResourceLocation CARD_TEXTURE =
            new ResourceLocation("createtreasury", "textures/gui/card_gui.png");

    public static final int CARD_GUI_W = 220;
    public static final int CARD_GUI_H = 157;

    // Create mod alert icon row 2, column 5 of create:textures/gui/icons.png (16×16 each)
    public static final ResourceLocation CREATE_ICONS =
            new ResourceLocation("create", "textures/gui/icons.png");
    public static final int ALERT_ICON_U    = (5 - 1) * 16; // 64
    public static final int ALERT_ICON_V    = (2 - 1) * 16; // 16
    public static final int ALERT_ICON_SIZE = 16;

    public static final int GUI_WIDTH   = 226;
    public static final int GUI_HEIGHT  = 191;
    public static final int GUI_OFFSET  = 13;  // border in the texture before the panel starts

    public static final int HOVER_COLOR = 0x887878b8;

    // Input field sprite 18x18, 18px above the normal button
    public static final int INPUT_U      = 238;
    public static final int INPUT_V      = 220;
    public static final int INPUT_W      = 18;
    public static final int INPUT_H      = 18;
    public static final int INPUT_CORNER = 2;

    /** 9-slice the input field sprite behind a text box of any size. */
    public static void blitInputField(GuiGraphics g, int x, int y, int w, int h) {
        blitNineSlice(g, TEXTURE, x, y, w, h, INPUT_U, INPUT_V, INPUT_W, INPUT_H, INPUT_CORNER);
    }

    /** Same as blitInputField but draws from the card_gui texture. */
    public static void blitCardInputField(GuiGraphics g, int x, int y, int w, int h) {
        blitNineSlice(g, CARD_TEXTURE, x, y, w, h, INPUT_U, INPUT_V, INPUT_W, INPUT_H, INPUT_CORNER);
    }

    // Normal button sprite (bottom-right of 256x256 texture)
    public static final int BTN_U        = 238;
    public static final int BTN_V        = 238;
    // Highlighted/hovered button sprite
    public static final int BTN_HOVER_U  = 220;
    public static final int BTN_HOVER_V  = 238;
    public static final int BTN_W             = 18;
    public static final int BTN_H             = 18;
    public static final int BTN_CORNER        = 2;

    protected int leftPos;
    protected int topPos;

    protected TreasuryScreenBase(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        leftPos = (width  - GUI_WIDTH)  / 2;
        topPos  = (height - GUI_HEIGHT) / 2;
    }

    protected void renderTreasuryBackground(GuiGraphics graphics) {
        graphics.blit(TEXTURE, leftPos, topPos, GUI_OFFSET, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }

    /** Renders the brass title and dynamic Employment label that appear on every screen. */
    protected void renderStandardHeader(GuiGraphics graphics) {
        graphics.drawString(font, Censor.apply("Company Terminal"), leftPos + 4, topPos + 4, 0x3d2a00, false);
        graphics.drawString(font, Censor.apply("Employment: " + employmentText()), leftPos + 7, topPos + 21, 0xc8b090, false);
    }

    private static String employmentText() {
        boolean hasC = ClientCompanyCache.hasCompany();
        boolean hasB = ClientCompanyCache.hasBank();
        if (hasC && hasB)  return ClientCompanyCache.companyName + " & " + ClientCompanyCache.bankName;
        if (hasC)          return ClientCompanyCache.companyName;
        if (hasB)          return ClientCompanyCache.bankName + " [Bank]";
        return "Unemployed";
    }

    /**
     * Blits a single source region stretched to the destination rectangle.
     * Uses a temporary pose scale so the rest of the matrix is unaffected.
     */
    public static void blitStretched(GuiGraphics g, ResourceLocation tex,
                                     int dx, int dy, int dw, int dh,
                                     int u,  int v,  int sw, int sh) {
        if (dw <= 0 || dh <= 0) return;
        g.pose().pushPose();
        g.pose().translate(dx, dy, 0);
        g.pose().scale((float) dw / sw, (float) dh / sh, 1f);
        g.blit(tex, 0, 0, u, v, sw, sh, 256, 256);
        g.pose().popPose();
    }

    /**
     * 9-slice blit: corners are fixed, edges/center stretch to fill dw x dh.
     *
     * @param u,v     top-left UV of the source cell in the texture
     * @param cw,ch   pixel dimensions of the source cell
     * @param corner  size of each fixed corner (same for all four)
     */
    public static void blitNineSlice(GuiGraphics g, ResourceLocation tex,
                                     int dx, int dy, int dw, int dh,
                                     int u, int v, int cw, int ch, int corner) {
        int midSrcW = cw - corner * 2;
        int midSrcH = ch - corner * 2;
        int midDstW = dw - corner * 2;
        int midDstH = dh - corner * 2;

        // --- corners (fixed size, no scaling) ---
        g.blit(tex, dx,               dy,               u,             v,             corner, corner, 256, 256);
        g.blit(tex, dx + dw - corner, dy,               u + cw - corner, v,           corner, corner, 256, 256);
        g.blit(tex, dx,               dy + dh - corner, u,             v + ch - corner, corner, corner, 256, 256);
        g.blit(tex, dx + dw - corner, dy + dh - corner, u + cw - corner, v + ch - corner, corner, corner, 256, 256);

        // --- edges (one axis stretched) ---
        blitStretched(g, tex, dx + corner, dy,               midDstW, corner,  u + corner, v,               midSrcW, corner);  // top
        blitStretched(g, tex, dx + corner, dy + dh - corner, midDstW, corner,  u + corner, v + ch - corner, midSrcW, corner);  // bottom
        blitStretched(g, tex, dx,               dy + corner, corner,  midDstH, u,               v + corner, corner,  midSrcH); // left
        blitStretched(g, tex, dx + dw - corner, dy + corner, corner,  midDstH, u + cw - corner, v + corner, corner,  midSrcH); // right

        // --- center (both axes stretched) ---
        blitStretched(g, tex, dx + corner, dy + corner, midDstW, midDstH, u + corner, v + corner, midSrcW, midSrcH);
    }

    // Button that uses 9-slice to scale the texture cell cleanly
    public static class TreasuryButton extends AbstractButton {

        private final Runnable         action;
        final         ResourceLocation tex;

        public TreasuryButton(int x, int y, int w, int h, Component label, Runnable action) {
            this(x, y, w, h, label, action, TEXTURE);
        }

        public TreasuryButton(int x, int y, int w, int h, Component label, Runnable action,
                              ResourceLocation texture) {
            super(x, y, w, h, label);
            this.action = action;
            this.tex    = texture;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int u = isHovered() ? BTN_HOVER_U : BTN_U;
            int v = isHovered() ? BTN_HOVER_V : BTN_V;
            blitNineSlice(graphics, tex,
                    getX(), getY(), width, height,
                    u, v, BTN_W, BTN_H, BTN_CORNER);

            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    getX() + width / 2,
                    getY() + (height - 8) / 2,
                    0xE0E0E0
            );
        }

        @Override
        public void onPress() { action.run(); }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }

    /** TreasuryButton that stays highlighted when selected (for radio-style groups). */
    public static class ToggleTreasuryButton extends TreasuryButton {

        private boolean selected;

        public ToggleTreasuryButton(int x, int y, int w, int h, Component label, Runnable action) {
            super(x, y, w, h, label, action, TEXTURE);
        }

        public void setSelected(boolean selected) { this.selected = selected; }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int u = (isHovered() || selected) ? BTN_HOVER_U : BTN_U;
            int v = (isHovered() || selected) ? BTN_HOVER_V : BTN_V;
            blitNineSlice(graphics, tex, getX(), getY(), width, height, u, v, BTN_W, BTN_H, BTN_CORNER);
            graphics.drawCenteredString(
                    Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, 0xE0E0E0);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
}
