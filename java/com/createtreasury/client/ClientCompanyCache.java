package com.createtreasury.client;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the current player's company/bank state on the client, synced from the server.
 *
 * Company slot  → type = "Company"
 * Bank slot     → type = "Bank"
 * Pending invite is a shared single slot (a player can only have one pending invite at a time).
 */
public class ClientCompanyCache {

    // ---- Company slot ----
    @Nullable public static String       companyName       = null;
    @Nullable public static String       tagline           = null;
    @Nullable public static String       industry          = null;
    public static           boolean      isOwner           = false;
    @Nullable public static String       ownerName         = null;
    public static           List<String> memberNames       = new ArrayList<>();

    // ---- Bank slot ----
    @Nullable public static String       bankName          = null;
    @Nullable public static String       bankTagline       = null;
    @Nullable public static String       bankIndustry      = null;
    public static           boolean      bankIsOwner       = false;
    @Nullable public static String       bankOwnerName     = null;
    public static           List<String> bankMemberNames   = new ArrayList<>();

    // ---- Shared pending invite ----
    @Nullable public static String       pendingInviteFrom = null; // company name of pending invite

    // ---- Basic helpers ----

    public static boolean hasCompany()      { return companyName != null; }
    public static boolean hasBank()         { return bankName    != null; }
    public static boolean hasPendingInvite(){ return pendingInviteFrom != null; }

    /** Returns true if the player is in an entity of the given type. */
    public static boolean hasEntity(String type) {
        return "Bank".equals(type) ? hasBank() : hasCompany();
    }

    // ---- Type-routed helpers ----

    @Nullable
    public static String getName(String type) {
        return "Bank".equals(type) ? bankName : companyName;
    }

    @Nullable
    public static String getTaglineFor(String type) {
        return "Bank".equals(type) ? bankTagline : tagline;
    }

    @Nullable
    public static String getIndustryFor(String type) {
        return "Bank".equals(type) ? bankIndustry : industry;
    }

    public static boolean isOwnerOf(String type) {
        return "Bank".equals(type) ? bankIsOwner : isOwner;
    }

    @Nullable
    public static String getOwnerNameFor(String type) {
        return "Bank".equals(type) ? bankOwnerName : ownerName;
    }

    public static List<String> getMemberNamesFor(String type) {
        return "Bank".equals(type) ? bankMemberNames : memberNames;
    }

    // ---- Update ----

    /**
     * Called by SyncCompanyPacket on the client thread.
     *
     * Routing rules:
     *  - type = "Bank"    → update the Bank slot
     *  - type = "Company" → update the Company slot
     *  - type = null      → ONLY update the pendingInviteFrom field (Company/Bank slots untouched)
     *
     * When kicking/leaving, the server always passes the entity's type and a null name to clear
     * that specific slot, while preserving the other slot and the pending invite separately.
     */
    public static void update(@Nullable String name, @Nullable String tagline, @Nullable String industry,
                              @Nullable String type, boolean ownerFlag, @Nullable String ownerNameVal,
                              List<String> members, @Nullable String pendingFrom) {
        if (type == null) {
            // Only the pending invite field changes (decline / invite-only packet)
            ClientCompanyCache.pendingInviteFrom = pendingFrom;
            return;
        }

        if ("Bank".equals(type)) {
            ClientCompanyCache.bankName        = name;
            ClientCompanyCache.bankTagline     = tagline;
            ClientCompanyCache.bankIndustry    = industry;
            ClientCompanyCache.bankIsOwner     = ownerFlag;
            ClientCompanyCache.bankOwnerName   = ownerNameVal;
            ClientCompanyCache.bankMemberNames = new ArrayList<>(members);
        } else {
            // "Company" (or any other value)
            ClientCompanyCache.companyName = name;
            ClientCompanyCache.tagline     = tagline;
            ClientCompanyCache.industry    = industry;
            ClientCompanyCache.isOwner     = ownerFlag;
            ClientCompanyCache.ownerName   = ownerNameVal;
            ClientCompanyCache.memberNames = new ArrayList<>(members);
        }

        // Accept packets carry pendingFrom=null to clear the pending invite.
        // Kick/Leave packets carry the player's preserved pending invite so it isn't lost.
        // Either way, update it.
        ClientCompanyCache.pendingInviteFrom = pendingFrom;
    }

    public static void clear() {
        companyName = null; tagline = null; industry = null;
        isOwner = false; ownerName = null; memberNames = new ArrayList<>();
        bankName = null; bankTagline = null; bankIndustry = null;
        bankIsOwner = false; bankOwnerName = null; bankMemberNames = new ArrayList<>();
        pendingInviteFrom = null;
    }
}
