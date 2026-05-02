package com.createtreasury.economy;

import com.createtreasury.config.TreasuryServerConfig;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

public class CardUtils {

    // ── Card NBT keys ─────────────────────────────────────────────────────────
    public static final String TAG_ISSUED_BY    = "IssuedBy";
    public static final String TAG_HOLDER_UUID  = "HolderUUID";
    public static final String TAG_HOLDER_NAME  = "HolderName";
    public static final String TAG_ACTIVE       = "Active";
    public static final String TAG_PIN          = "PIN";
    public static final String TAG_LOCKED       = "Locked";
    public static final String TAG_PIN_ATTEMPTS = "PinAttempts";

    // ── Coin value fallback constants (used when config is unavailable) ───────
    // Hierarchy: 1 Brass = 10 Zinc = 100 Andesite (Andesite is the base/smallest coin)
    public static final int ANDESITE_VALUE  = 1;
    public static final int ZINC_VALUE      = 10;
    public static final int BRASS_VALUE     = 100;

    // ── Config-backed coin value methods ──────────────────────────────────────

    public static int zincValue() {
        try { return TreasuryServerConfig.SERVER.zincValue.get(); }
        catch (Exception e) { return ZINC_VALUE; }
    }

    public static int brassValue() {
        try { return TreasuryServerConfig.SERVER.brassValue.get(); }
        catch (Exception e) { return BRASS_VALUE; }
    }

    public static int andesiteValue() {
        try { return TreasuryServerConfig.SERVER.andesiteValue.get(); }
        catch (Exception e) { return ANDESITE_VALUE; }
    }

    // ── Card state helpers ────────────────────────────────────────────────────

    /** True if the card has no IssuedBy tag (fresh from crafting). */
    public static boolean isBlank(ItemStack stack) {
        return !stack.hasTag() || !stack.getTag().contains(TAG_ISSUED_BY);
    }

    /** True if the card was pressed by a Card Press (has IssuedBy tag). */
    public static boolean isIssued(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_ISSUED_BY);
    }

    /** True if the card has been engraved with a holder name by a Card Engraver. */
    public static boolean isEngraved(ItemStack stack) {
        return isIssued(stack) && stack.getTag().contains(TAG_HOLDER_NAME)
                && !stack.getTag().getString(TAG_HOLDER_NAME).isEmpty();
    }

    /** True if the card has been activated by a player (has Active=true). */
    public static boolean isActive(ItemStack stack) {
        return isEngraved(stack) && stack.getTag().getBoolean(TAG_ACTIVE);
    }

    /** Returns the issuing bank name, or null if the card is blank. */
    @Nullable
    public static String getIssuedBy(ItemStack stack) {
        return isIssued(stack) ? stack.getTag().getString(TAG_ISSUED_BY) : null;
    }

    /** Returns the engraved holder name, or null if the card hasn't been engraved yet. */
    @Nullable
    public static String getHolderName(ItemStack stack) {
        return isEngraved(stack) ? stack.getTag().getString(TAG_HOLDER_NAME) : null;
    }

    /** Returns the account holder's UUID, or null if not active or UUID is malformed. */
    @Nullable
    public static UUID getHolderUUID(ItemStack stack) {
        if (!isActive(stack)) return null;
        String uuidStr = stack.getTag().getString(TAG_HOLDER_UUID);
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Returns the card's PIN, or null if no PIN is set. */
    @Nullable
    public static String getPin(ItemStack stack) {
        if (!isActive(stack) || !stack.getTag().contains(TAG_PIN)) return null;
        String pin = stack.getTag().getString(TAG_PIN);
        return pin.isEmpty() ? null : pin;
    }

    /** True if the card has a PIN configured. */
    public static boolean hasPin(ItemStack stack) {
        return getPin(stack) != null;
    }

    /** True if the card has been locked (e.g. too many wrong PIN attempts). */
    public static boolean isLocked(ItemStack stack) {
        return isActive(stack) && stack.getTag().getBoolean(TAG_LOCKED);
    }

    /** Returns the number of consecutive wrong PIN attempts stored on the card. */
    public static int getPinAttempts(ItemStack stack) {
        if (!isActive(stack) || !stack.getTag().contains(TAG_PIN_ATTEMPTS)) return 0;
        return stack.getTag().getInt(TAG_PIN_ATTEMPTS);
    }

    // ── Currency formatting ───────────────────────────────────────────────────

    /**
     * Formats a balance (in base units) as a human-readable string using the
     * configured coin denominations, e.g. 137 → "5 Andesite, 2 Brass, 2 Zinc".
     */
    public static String formatBalance(long baseUnits) {
        if (baseUnits <= 0) return "0 Andesite";
        int bv = brassValue();    // largest (100)
        int zv = zincValue();     // middle  (10)
        int av = andesiteValue(); // base    (1)

        long brass    = baseUnits / bv;
        long rem      = baseUnits % bv;
        long zinc     = rem / zv;
        rem           = rem % zv;
        long andesite = rem / av;
        long leftover = rem % av;

        StringBuilder sb = new StringBuilder();
        if (brass > 0)    sb.append(brass).append(" Brass");
        if (zinc > 0)     { if (sb.length() > 0) sb.append(", "); sb.append(zinc).append(" Zinc"); }
        if (andesite > 0) { if (sb.length() > 0) sb.append(", "); sb.append(andesite).append(" Andesite"); }
        if (leftover > 0) { if (sb.length() > 0) sb.append(", "); sb.append(leftover).append(" base"); }
        return sb.length() == 0 ? "0 Andesite" : sb.toString();
    }
}
