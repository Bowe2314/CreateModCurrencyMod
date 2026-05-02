package com.createtreasury.gui;

import com.createtreasury.config.TreasuryServerConfig;
import com.createtreasury.economy.CardUtils;
import com.createtreasury.network.DepositPacket;
import com.createtreasury.network.LockCardPacket;
import com.createtreasury.network.TreasuryNetwork;
import com.createtreasury.network.WithdrawPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BankingScreen extends Screen {

    private static final int W = TreasuryScreenBase.GUI_WIDTH;
    private static final int H = TreasuryScreenBase.GUI_HEIGHT;

    // Three denomination buttons span the full 200 px content width.
    // 64 + 4 gap + 64 + 4 gap + 64 = 200
    private static final int DENOM_BTN_W = 64;
    private static final int DENOM_GAP   = 4;

    private final String  bankName;
    private       long    balance;
    private final boolean requirePin;

    private boolean pinVerified;
    private boolean pendingRebuild   = false;
    private int     wrongPinAttempts = 0;
    private EditBox pinGateField;
    private String  pinError = "";

    /** Quantity input shared by all three denomination buttons. */
    private EditBox qtyField;

    public BankingScreen(String bankName, long balance, boolean requirePin) {
        super(Component.translatable("createtreasury.screen.banking"));
        this.bankName    = bankName;
        this.balance     = balance;
        this.requirePin  = requirePin;
        this.pinVerified = !requirePin;
    }

    public void updateBalance(long newBalance) {
        this.balance = newBalance;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = (width  - W) / 2;
        int y = (height - H) / 2;
        if (!pinVerified) {
            initPinGate(x, y);
        } else {
            initMainWidgets(x, y);
        }
    }

    // ── PIN gate ──────────────────────────────────────────────────────────────

    private void initPinGate(int x, int y) {
        pinGateField = new EditBox(font, x + 15, y + 80, 196, 12,
                Component.literal("PIN"));
        pinGateField.setMaxLength(8);
        pinGateField.setFilter(s -> s.matches("\\d{0,8}"));
        pinGateField.setHint(Component.literal("Enter PIN"));
        pinGateField.setBordered(false);
        addRenderableWidget(pinGateField);

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 13, y + 100, 200, 18,
                Component.translatable("createtreasury.screen.confirm_pin"),
                this::onPinConfirm));

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 13, y + 165, 200, 18,
                Component.translatable("gui.cancel"),
                this::onClose));
    }

    private void onPinConfirm() {
        if (pinGateField == null) return;
        String entered = pinGateField.getValue().trim();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack card = findActiveCard(mc.player);
        if (card.isEmpty()) { onClose(); return; }

        String stored = CardUtils.getPin(card);
        if (stored != null && stored.equals(entered)) {
            pinVerified    = true;
            pinError       = "";
            pendingRebuild = true;
        } else {
            wrongPinAttempts++;
            int maxAttempts = TreasuryServerConfig.SERVER.maxPinAttempts.get();
            if (maxAttempts > 0 && wrongPinAttempts >= maxAttempts) {
                TreasuryNetwork.CHANNEL.sendToServer(new LockCardPacket());
                onClose();
                return;
            }
            int remaining = maxAttempts > 0 ? maxAttempts - wrongPinAttempts : -1;
            pinError = remaining > 0
                    ? "Incorrect PIN. " + remaining + " attempt(s) left."
                    : "Incorrect PIN. Try again.";
        }
    }

    // ── Main widgets ──────────────────────────────────────────────────────────

    private void initMainWidgets(int x, int y) {
        // Quantity input (how many of each coin to withdraw)
        qtyField = new EditBox(font, x + 15, y + 92, 196, 12,
                Component.literal("Qty"));
        qtyField.setHint(Component.literal("Quantity (blank = 1)"));
        qtyField.setMaxLength(9);
        qtyField.setFilter(s -> s.matches("\\d{0,9}"));
        qtyField.setBordered(false);
        addRenderableWidget(qtyField);

        // Denomination buttons  (Brass / Zinc / Andesite)
        int bx = x + 13;
        int by = y + 111;
        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                bx, by, DENOM_BTN_W, 18,
                Component.literal("Brass"),
                () -> onWithdrawDenom(WithdrawPacket.DENOM_BRASS)));

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                bx + DENOM_BTN_W + DENOM_GAP, by, DENOM_BTN_W, 18,
                Component.literal("Zinc"),
                () -> onWithdrawDenom(WithdrawPacket.DENOM_ZINC)));

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                bx + (DENOM_BTN_W + DENOM_GAP) * 2, by, DENOM_BTN_W, 18,
                Component.literal("Andesite"),
                () -> onWithdrawDenom(WithdrawPacket.DENOM_ANDESITE)));

        // Deposit + Done
        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 13, y + 133, 200, 18,
                Component.translatable("createtreasury.screen.deposit_all"),
                this::onDepositAll));

        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 13, y + 166, 200, 18,
                Component.translatable("gui.done"),
                this::onClose));
    }

    /**
     * Withdraws qty coins of the given denomination type (DENOM_* constant).
     * Blank qty means 1. The server caps to the real balance.
     */
    private void onWithdrawDenom(int denomType) {
        long qty = 1;
        if (qtyField != null) {
            String text = qtyField.getValue().trim();
            if (!text.isEmpty()) {
                try { qty = Long.parseLong(text); }
                catch (NumberFormatException ignored) { return; }
            }
        }
        if (qty <= 0) return;
        TreasuryNetwork.CHANNEL.sendToServer(new WithdrawPacket(qty, denomType));
        if (qtyField != null) qtyField.setValue("");
    }

    private void onDepositAll() {
        TreasuryNetwork.CHANNEL.sendToServer(new DepositPacket());
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        if (pendingRebuild) {
            pendingRebuild = false;
            init();
        }
        renderBackground(gfx);

        int x = (width  - W) / 2;
        int y = (height - H) / 2;
        gfx.blit(TreasuryScreenBase.TEXTURE, x, y,
                TreasuryScreenBase.GUI_OFFSET, 0, W, H, 256, 256);

        if (!pinVerified) {
            gfx.drawString(font, "Brass ATM",                       x + 4, y + 4,  0x3d2a00, false);
            gfx.drawString(font, bankName,                          x + 7, y + 20, 0xc8b090, false);
            gfx.drawString(font, "Enter your PIN to continue:",     x + 6, y + 44, 0xc8b090, false);
            TreasuryScreenBase.blitInputField(gfx, x + 13, y + 75, 200, 18);
            if (!pinError.isEmpty()) {
                gfx.drawString(font, pinError, x + 6, y + 122, 0xff5555, false);
            }
        } else {
            gfx.drawString(font, "Brass ATM",                       x + 4, y + 4,  0x3d2a00, false);
            gfx.drawString(font, bankName,                          x + 7, y + 20, 0xc8b090, false);
            gfx.drawString(font, "Balance:",                        x + 6, y + 44, 0xc8b090, false);
            gfx.drawString(font, CardUtils.formatBalance(balance),  x + 6, y + 55, 0xffffff, false);
            gfx.drawString(font, "Qty to withdraw:",                x + 6, y + 75, 0xc8b090, false);
            TreasuryScreenBase.blitInputField(gfx, x + 13, y + 87, 200, 18);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter on PIN gate confirms PIN
        if ((keyCode == 257 || keyCode == 335)
                && !pinVerified && pinGateField != null && pinGateField.isFocused()) {
            onPinConfirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static ItemStack findActiveCard(net.minecraft.world.entity.player.Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (CardUtils.isActive(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
