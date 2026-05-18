package com.example.craftmaker.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Конфигурация мода CraftMaker.
 */
public class CraftMakerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue showWelcomeMessage;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        
        showWelcomeMessage = builder
                .define("showWelcomeMessage", true);
        
        builder.pop();

        SPEC = builder.build();
    }
}
