package com.createtreasury.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class TreasuryConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client          CLIENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(builder);
        CLIENT_SPEC = builder.build();
    }

    public static class Client {

        public final ForgeConfigSpec.BooleanValue ergophobia;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Create: Treasury client settings").push("client");

            ergophobia = builder
                    .comment("Replace work-related words (job, employment, company, …) with asterisks of the same length.")
                    .define("ergophobia", false);

            builder.pop();
        }
    }
}
