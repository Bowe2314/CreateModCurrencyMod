package com.createtreasury.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class TreasuryServerConfig {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server          SERVER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    public static class Server {

        // ── Company / Bank membership ─────────────────────────────────────────
        public final ForgeConfigSpec.BooleanValue allowDualMembership;

        // ── Minting Press linking ───────────────────────────────────────────
        public final ForgeConfigSpec.BooleanValue requireLinkedBank;
        public final ForgeConfigSpec.BooleanValue enableMaxLinksPerBank;
        public final ForgeConfigSpec.IntValue     maxLinksPerBank;

        // ── Minting Press coin tagging ──────────────────────────────────────
        public final ForgeConfigSpec.BooleanValue coinTaggingEnabled;
        public final ForgeConfigSpec.BooleanValue stripTagOnTransfer;

        // ── Minting Press production ────────────────────────────────────────
        public final ForgeConfigSpec.BooleanValue requireOwnerOnline;

        // ── Card system PIN ─────────────────────────────────────────────────
        public final ForgeConfigSpec.BooleanValue enablePins;
        public final ForgeConfigSpec.BooleanValue requirePinOnActivation;
        public final ForgeConfigSpec.IntValue     pinMinLength;
        public final ForgeConfigSpec.IntValue     pinMaxLength;
        /** 0 = unlimited attempts; > 0 = card is locked after this many wrong guesses. */
        public final ForgeConfigSpec.IntValue     maxPinAttempts;

        // ── Card system engraving ───────────────────────────────────────────
        /** Only members of the issuing bank may engrave a card. */
        public final ForgeConfigSpec.BooleanValue requireBankMemberToEngrave;
        /** Allow the engraved holder name to be overwritten on an already-engraved card. */
        public final ForgeConfigSpec.BooleanValue allowReEngrave;

        // ── Card system accounts ────────────────────────────────────────────
        /** Zinc-coin equivalent credited automatically when an account is first created. */
        public final ForgeConfigSpec.LongValue    startingBalance;
        /** Maximum account balance in zinc-coin units. 0 = no limit. */
        public final ForgeConfigSpec.LongValue    maxBalance;
        /** Whether a player may hold more than one active card from the same bank. */
        public final ForgeConfigSpec.BooleanValue allowMultipleCardsPerBank;

        // ── Card system transactions ────────────────────────────────────────
        public final ForgeConfigSpec.LongValue    minWithdrawal;
        /** 0 = no per-transaction cap. */
        public final ForgeConfigSpec.LongValue    maxWithdrawalPerTx;
        /** Percentage of each withdrawal taken as a fee (0.0–100.0). */
        public final ForgeConfigSpec.DoubleValue  withdrawalFeePercent;
        /** Percentage of each deposit taken as a fee (0.0–100.0). */
        public final ForgeConfigSpec.DoubleValue  depositFeePercent;

        // ── Coin denominations ────────────────────────────────────────────────
        public final ForgeConfigSpec.IntValue zincValue;
        public final ForgeConfigSpec.IntValue brassValue;
        public final ForgeConfigSpec.IntValue andesiteValue;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Create: Treasury server settings").push("server");

            // ── Company / Bank membership ─────────────────────────────────────
            builder.comment("Company and Bank membership rules").push("membership");
            allowDualMembership = builder
                    .comment("Allow players to be in both a Company and a Bank simultaneously.",
                             "Default: false.")
                    .define("allowDualMembership", false);
            builder.pop();

            // ── Minting Press ─────────────────────────────────────────────────
            builder.comment("Minting Press behaviour").push("minting_press");

            requireLinkedBank = builder
                    .comment("Require a Minting Press to be linked to a Bank before minting.")
                    .define("requireLinkedBank", false);

            builder.push("max_links");
            enableMaxLinksPerBank = builder
                    .comment("Cap how many Minting Presses a single Bank may be linked to.")
                    .define("enableMaxLinksPerBank", false);
            maxLinksPerBank = builder
                    .comment("Maximum linked Minting Presses per Bank (1–64). Requires enableMaxLinksPerBank.")
                    .defineInRange("maxLinksPerBank", 3, 1, 64);
            builder.pop();

            builder.push("coin_tagging");
            coinTaggingEnabled = builder
                    .comment("Stamp coins minted by a linked press with 'MintedBy: <BankName>' NBT.")
                    .define("coinTaggingEnabled", true);
            stripTagOnTransfer = builder
                    .comment("Strip the MintedBy tag when a non-member picks up the coin.")
                    .define("stripTagOnTransfer", false);
            builder.pop();

            builder.push("production");
            requireOwnerOnline = builder
                    .comment("A linked Minting Press only operates while at least one Bank member is online.")
                    .define("requireOwnerOnline", false);
            builder.pop();

            builder.pop(); // minting_press

            // ── Card system ───────────────────────────────────────────────────
            builder.comment("Debit card system").push("card_system");

            // PIN
            builder.comment("PIN security").push("pin");
            enablePins = builder
                    .comment("Allow players to set a PIN when activating their card.",
                             "The PIN is requested each time the card is used at a Brass ATM.")
                    .define("enablePins", false);
            requirePinOnActivation = builder
                    .comment("Require a non-empty PIN during activation (ignored when enablePins=false).")
                    .define("requirePinOnActivation", false);
            pinMinLength = builder
                    .comment("Minimum PIN length in digits (1–8).")
                    .defineInRange("pinMinLength", 4, 1, 8);
            pinMaxLength = builder
                    .comment("Maximum PIN length in digits (1–8, must be >= pinMinLength).")
                    .defineInRange("pinMaxLength", 8, 1, 8);
            maxPinAttempts = builder
                    .comment("Lock the card after this many consecutive wrong PIN attempts.",
                             "0 = unlimited attempts (no lockout).")
                    .defineInRange("maxPinAttempts", 0, 0, 20);
            builder.pop(); // pin

            // Engraving
            builder.comment("Card engraving rules").push("engraving");
            requireBankMemberToEngrave = builder
                    .comment("Only a member of the card's issuing bank may use the Card Engraver.",
                             "Prevents third parties from personalising cards they do not control.")
                    .define("requireBankMemberToEngrave", false);
            allowReEngrave = builder
                    .comment("Allow overwriting the holder name on an already-engraved card.",
                             "Default: false once engraved, the holder name is permanent.")
                    .define("allowReEngrave", false);
            builder.pop(); // engraving

            // Accounts
            builder.comment("Account settings").push("accounts");
            startingBalance = builder
                    .comment("Zinc-coin equivalent automatically deposited when an account is first opened.",
                             "0 = no welcome bonus.")
                    .defineInRange("startingBalance", 0L, 0L, Long.MAX_VALUE);
            maxBalance = builder
                    .comment("Maximum account balance in zinc-coin units.",
                             "0 = no limit. Deposits that would exceed this are partially rejected.")
                    .defineInRange("maxBalance", 0L, 0L, Long.MAX_VALUE);
            allowMultipleCardsPerBank = builder
                    .comment("Allow a player to hold more than one active card from the same bank.",
                             "Default: false only one active card per bank per player.")
                    .define("allowMultipleCardsPerBank", false);
            builder.pop(); // accounts

            // Transactions
            builder.comment("Transaction limits and fees").push("transactions");
            minWithdrawal = builder
                    .comment("Minimum withdrawal amount in zinc-coin units.")
                    .defineInRange("minWithdrawal", 1L, 1L, Long.MAX_VALUE);
            maxWithdrawalPerTx = builder
                    .comment("Maximum withdrawal per transaction in zinc-coin units.",
                             "0 = no per-transaction cap.")
                    .defineInRange("maxWithdrawalPerTx", 0L, 0L, Long.MAX_VALUE);
            withdrawalFeePercent = builder
                    .comment("Percentage of each withdrawal taken as a bank fee (0.0–100.0).",
                             "E.g. 2.5 means 2.5 % of the withdrawn amount is deducted additionally.",
                             "Fees are rounded down. 0 = no fee.")
                    .defineInRange("withdrawalFeePercent", 0.0, 0.0, 100.0);
            depositFeePercent = builder
                    .comment("Percentage of each deposit taken as a bank fee (0.0–100.0).",
                             "E.g. 1.0 means only 99 % of deposited coins reach your account.",
                             "Fees are rounded down. 0 = no fee.")
                    .defineInRange("depositFeePercent", 0.0, 0.0, 100.0);
            builder.pop(); // transactions

            builder.pop(); // card_system

            // ── Coin denominations ────────────────────────────────────────────
            builder.comment("Coin denomination values in base units.",
                            "These affect deposits, withdrawals, and balance display.",
                            "Change with care existing balances are stored in the old base unit.").push("coin_denominations");
            andesiteValue = builder
                    .comment("Value of one Andesite Coin in base units. Andesite is the base (smallest) coin.")
                    .defineInRange("andesiteValue", 1, 1, 10000);
            zincValue = builder
                    .comment("Value of one Zinc Coin in base units. Default: 10 Andesite.")
                    .defineInRange("zincValue", 10, 1, 10000);
            brassValue = builder
                    .comment("Value of one Brass Coin in base units. Default: 10 Zinc = 100 Andesite.")
                    .defineInRange("brassValue", 100, 1, 10000);
            builder.pop(); // coin_denominations

            builder.pop(); // server
        }
    }
}
