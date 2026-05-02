package com.createtreasury.util;

import net.createmod.catnip.utility.lang.LangBuilder;

/** Thin wrapper around {@link LangBuilder} pre-set to the "createtreasury" namespace. */
public class TreasuryLang {

    public static LangBuilder translate(String key, Object... args) {
        return new LangBuilder("createtreasury").translate(key, args);
    }

    public static LangBuilder text(String literal) {
        return new LangBuilder("createtreasury").text(literal);
    }
}
