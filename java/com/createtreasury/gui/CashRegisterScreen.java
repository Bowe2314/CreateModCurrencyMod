package com.createtreasury.gui;

import com.createtreasury.economy.CardUtils;
import com.createtreasury.network.CashRegisterBuyPacket;
import com.createtreasury.network.CashRegisterCollectPacket;
import com.createtreasury.network.CashRegisterSetPricesPacket;
import com.createtreasury.network.CashRegisterSetSlotPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class CashRegisterScreen extends Screen {

    // Layout constants (same panel as BankingScreen)
    private static final int W = TreasuryScreenBase.GUI_WIDTH;   // 226
    private static final int H = TreasuryScreenBase.GUI_HEIGHT;  // 191

    // Three 64-px columns with 4-px gaps starting at panel-left+13
    private static final int COL_W    = 64;
    private static final int COL_GAP  = 4;
    private static final int COL_LEFT = 13;

    // Column left-edges relative to panel origin
    private static final int[] COL_X = {
            COL_LEFT,
            COL_LEFT + COL_W + COL_GAP,
            COL_LEFT + (COL_W + COL_GAP) * 2
    };

    // ── State ─────────────────────────────────────────────────────────────────

    private final BlockPos   pos;
    private final ItemStack[] shopItems;
    private final long[]      prices;
    private       long        coinStash;
    @Nullable
    private final String      linkedCompany;
    private final boolean     isOwner;

    /** Buyer payment mode: 0 = coins, 1 = card. */
    private int payMode = CashRegisterBuyPacket.PAY_COIN;

    /** Owner price edit fields (one per slot). */
    private EditBox[] priceFields;

    /** Whether a full widget rebuild is needed on the next render pass. */
    private boolean pendingRebuild = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CashRegisterScreen(BlockPos pos, ItemStack[] shopItems, long[] prices,
                               long coinStash, @Nullable String linkedCompany, boolean isOwner) {
        super(Component.translatable("createtreasury.screen.cash_register"));
        this.pos           = pos;
        this.shopItems     = shopItems;
        this.prices        = prices;
        this.coinStash     = coinStash;
        this.linkedCompany = linkedCompany;
        this.isOwner       = isOwner;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        clearWidgets();
        int x = (width  - W) / 2;
        int y = (height - H) / 2;

        if (isOwner) {
            initOwnerWidgets(x, y);
        } else {
            initBuyerWidgets(x, y);
        }
    }

    // ── Owner widgets ─────────────────────────────────────────────────────────

    private void initOwnerWidgets(int x, int y) {
        // Collect button (top-right area)
        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + 138, y + 28, 75, 18,
                Component.literal("Collect"),
                this::onCollect));

        // Set / Clear buttons for each slot
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            int cx = x + COL_X[i];
            // "Set" button (hold item in hand, then click to store)
            addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                    cx + 12, y + 72, 40, 14,
                    Component.literal(shopItems[i].isEmpty() ? "Set" : "Restock"),
                    () -> TreasuryNetwork.CHANNEL.sendToServer(
                            new CashRegisterSetSlotPacket(pos, slot))));
        }

        // Price edit fields
        priceFields = new EditBox[3];
        for (int i = 0; i < 3; i++) {
            int cx = x + COL_X[i];
            priceFields[i] = new EditBox(font, cx + 8, y + 92, 48, 12,
                    Component.literal("Price"));
            priceFields[i].setMaxLength(9);
            priceFields[i].setFilter(s -> s.matches("\\d{0,9}"));
            priceFields[i].setBordered(false);
            priceFields[i].setHint(Component.literal("0"));
            priceFields[i].setValue(prices[i] > 0 ? String.valueOf(prices[i]) : "");
            addRenderableWidget(priceFields[i]);
        }

        // Save Prices button
        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + COL_LEFT, y + 108, 200, 18,
                Component.translatable("createtreasury.screen.save_prices"),
                this::onSavePrices));

        // Done button
        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + COL_LEFT, y + 166, 200, 18,
                Component.translatable("gui.done"),
                this::onClose));
    }

    // ── Buyer widgets ─────────────────────────────────────────────────────────

    private void initBuyerWidgets(int x, int y) {
        // Payment mode toggle
        TreasuryScreenBase.ToggleTreasuryButton coinBtn =
                new TreasuryScreenBase.ToggleTreasuryButton(
                        x + COL_LEFT, y + 28, 98, 18,
                        Component.literal("Pay: Coins"),
                        () -> setPayMode(CashRegisterBuyPacket.PAY_COIN));
        TreasuryScreenBase.ToggleTreasuryButton cardBtn =
                new TreasuryScreenBase.ToggleTreasuryButton(
                        x + COL_LEFT + 98 + 4, y + 28, 98, 18,
                        Component.literal("Pay: Card"),
                        () -> setPayMode(CashRegisterBuyPacket.PAY_CARD));
        coinBtn.setSelected(payMode == CashRegisterBuyPacket.PAY_COIN);
        cardBtn.setSelected(payMode == CashRegisterBuyPacket.PAY_CARD);
        addRenderableWidget(coinBtn);
        addRenderableWidget(cardBtn);

        // Buy buttons for each slot
        for (int i = 0; i < 3; i++) {
            final int slot  = i;
            int       cx    = x + COL_X[i];
            boolean   avail = !shopItems[i].isEmpty() && prices[i] > 0;

            TreasuryScreenBase.TreasuryButton buyBtn =
                    new TreasuryScreenBase.TreasuryButton(
                            cx + 8, y + 100, 48, 18,
                            Component.literal("Buy"),
                            () -> TreasuryNetwork.CHANNEL.sendToServer(
                                    new CashRegisterBuyPacket(pos, slot, payMode)));
            buyBtn.active = avail;
            addRenderableWidget(buyBtn);
        }

        // Done button
        addRenderableWidget(new TreasuryScreenBase.TreasuryButton(
                x + COL_LEFT, y + 166, 200, 18,
                Component.translatable("gui.done"),
                this::onClose));
    }

    private void setPayMode(int mode) {
        payMode        = mode;
        pendingRebuild = true;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onSavePrices() {
        long[] newPrices = new long[3];
        for (int i = 0; i < 3; i++) {
            if (priceFields == null || priceFields[i] == null) continue;
            String text = priceFields[i].getValue().trim();
            if (!text.isEmpty()) {
                try { newPrices[i] = Math.max(0, Long.parseLong(text)); }
                catch (NumberFormatException ignored) {}
            }
        }
        TreasuryNetwork.CHANNEL.sendToServer(new CashRegisterSetPricesPacket(pos, newPrices));
    }

    private void onCollect() {
        TreasuryNetwork.CHANNEL.sendToServer(new CashRegisterCollectPacket(pos));
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

        // Panel background
        gfx.blit(TreasuryScreenBase.TEXTURE, x, y,
                TreasuryScreenBase.GUI_OFFSET, 0, W, H, 256, 256);

        // Title
        gfx.drawString(font, "Cash Register", x + 4, y + 4, 0x3d2a00, false);

        // Company / ownership line
        String compLine = linkedCompany != null ? linkedCompany : "Public shop";
        gfx.drawString(font, compLine, x + 7, y + 16, 0xc8b090, false);

        if (isOwner) {
            renderOwnerContent(gfx, x, y);
        } else {
            renderBuyerContent(gfx, x, y);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    private void renderOwnerContent(GuiGraphics gfx, int x, int y) {
        // Collect revenue line
        String stashText = coinStash > 0
                ? "Revenue: " + CardUtils.formatBalance(coinStash)
                : "Revenue: empty";
        gfx.drawString(font, stashText, x + 7, y + 33, 0xc8b090, false);

        // Slot columns
        for (int i = 0; i < 3; i++) {
            int cx = x + COL_X[i];

            // Slot label
            gfx.drawCenteredString(font, "Slot " + (i + 1), cx + COL_W / 2, y + 48, 0xc8b090);

            // Item icon background (18x18 input-field sprite)
            TreasuryScreenBase.blitInputField(gfx, cx + 23, y + 56, 18, 18);

            // Render item inside the box
            if (!shopItems[i].isEmpty()) {
                gfx.renderItem(shopItems[i], cx + 24, y + 57);
                gfx.renderItemDecorations(font, shopItems[i], cx + 24, y + 57);
            }

            // "Set/Restock" button is added in initOwnerWidgets

            // Price field background
            TreasuryScreenBase.blitInputField(gfx, cx + 7, y + 88, 50, 18);

            // Price hint label
            gfx.drawString(font, "Price:", cx + 8, y + 80, 0xc8b090, false);
        }

        // Save prices button label handled by widget itself

        // Hint
        gfx.drawString(font, "§7Hold item + click Set to stock", x + COL_LEFT, y + 132, 0xaaaaaa, false);
        gfx.drawString(font, "§7Empty hand + click Set to clear", x + COL_LEFT, y + 142, 0xaaaaaa, false);
    }

    private void renderBuyerContent(GuiGraphics gfx, int x, int y) {
        // Slot columns
        for (int i = 0; i < 3; i++) {
            int cx = x + COL_X[i];

            // Slot label
            gfx.drawCenteredString(font, "Slot " + (i + 1), cx + COL_W / 2, y + 50, 0xc8b090);

            // Item icon background
            TreasuryScreenBase.blitInputField(gfx, cx + 23, y + 58, 18, 18);

            if (!shopItems[i].isEmpty()) {
                gfx.renderItem(shopItems[i], cx + 24, y + 59);
                gfx.renderItemDecorations(font, shopItems[i], cx + 24, y + 59);

                // Item name (truncated)
                String name = shopItems[i].getHoverName().getString();
                if (name.length() > 9) name = name.substring(0, 8) + ".";
                gfx.drawCenteredString(font, name, cx + COL_W / 2, y + 78, 0xffffff);

                // Price
                String priceStr = prices[i] > 0
                        ? CardUtils.formatBalance(prices[i])
                        : "No price";
                gfx.drawCenteredString(font, "§e" + priceStr, cx + COL_W / 2, y + 88, 0xffffff);

                // Stock count
                String stockStr = "x" + shopItems[i].getCount();
                gfx.drawCenteredString(font, "§7" + stockStr, cx + COL_W / 2, y + 120, 0xaaaaaa);
            } else {
                gfx.drawCenteredString(font, "§8Empty", cx + COL_W / 2, y + 78, 0xaaaaaa);
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
