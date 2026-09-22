package com.bluoen.waveskimmer;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BASE_GENERATION = BUILDER
            .comment("Peak FE/tick generated on a big wave with fully open water around the skimmer.")
            .defineInRange("baseGeneration", 40, 0, 1_000_000);

    public static final ModConfigSpec.IntValue CAPACITY = BUILDER
            .comment("Internal energy buffer in FE.")
            .defineInRange("capacity", 100_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_OUTPUT = BUILDER
            .comment("Maximum FE/tick pushed into each connected cable or machine.")
            .defineInRange("maxOutput", 1_000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue OCEAN_MULTIPLIER = BUILDER
            .comment("Generation multiplier when floating in an ocean biome (bigger swells).")
            .defineInRange("oceanMultiplier", 1.5, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue RAIN_MULTIPLIER = BUILDER
            .comment("Generation multiplier while it is raining on the skimmer.")
            .defineInRange("rainMultiplier", 1.25, 0.0, 100.0);

    public static final ModConfigSpec.DoubleValue THUNDER_MULTIPLIER = BUILDER
            .comment("Generation multiplier during a thunderstorm (replaces the rain multiplier).")
            .defineInRange("thunderMultiplier", 1.75, 0.0, 100.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
