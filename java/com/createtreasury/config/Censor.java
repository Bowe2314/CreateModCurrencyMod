package com.createtreasury.config;

import java.util.regex.Pattern;

/** Replaces work-related words with asterisks when ergophobia mode is on. */
public class Censor {

    // Longer variants must come before their substrings so they match first.
    private static final String[] WORDS = {
        "unemployment", "unemployed", "employment", "employer", "employed",
        "manufacturing", "agriculture", "industries", "workforce", "industry",
        "companies", "company", "members", "working", "workers", "trading",
        "hiring", "salary", "member", "mining", "worker", "hired", "wages",
        "invite", "owner", "work", "hire", "wage", "jobs", "pay", "job"
    };

    private static final Pattern[] PATTERNS;

    static {
        PATTERNS = new Pattern[WORDS.length];
        for (int i = 0; i < WORDS.length; i++) {
            PATTERNS[i] = Pattern.compile("(?i)\\b" + Pattern.quote(WORDS[i]) + "\\b");
        }
    }

    public static String apply(String text) {
        if (!TreasuryConfig.CLIENT.ergophobia.get()) return text;
        String result = text;
        for (Pattern p : PATTERNS) {
            result = p.matcher(result).replaceAll(mr -> "*".repeat(mr.group().length()));
        }
        return result;
    }
}
