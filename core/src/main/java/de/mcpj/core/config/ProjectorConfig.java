package de.mcpj.core.config;

import de.mcpj.core.util.Units;

public final class ProjectorConfig {
    private final BooleanSetting requirePower;
    private final IntSetting energyPerJumper;
    private final IntSetting buildSeconds;
    private final IntSetting inputRate;
    private final IntSetting buffer;

    ProjectorConfig(ConfigBuilder builder) {
        builder.comment("Jumper projector: builds a jumper above itself after a redstone pulse").push("projector");
        requirePower = builder.comment("Whether the projector needs energy from cables (energy mods). If false, it builds for free")
                .define("requirePower", false);
        energyPerJumper = builder.comment("Energy for one jumper if requirePower is true (FE), used evenly over the build time")
                .defineInRange("energyPerJumper", 120_000_000, 1, Integer.MAX_VALUE);
        buildSeconds = builder.comment("Build time of one jumper (seconds)")
                .defineInRange("buildSeconds", 600, 1, 86_400);
        inputRate = builder.comment("Maximum input from cables (FE/tick)")
                .defineInRange("inputRate", 40_000, 1, Integer.MAX_VALUE);
        buffer = builder.comment("Internal buffer (FE)")
                .defineInRange("buffer", 400_000, 1, Integer.MAX_VALUE);
        builder.pop();
    }

    public boolean requirePower() {
        return requirePower.get();
    }

    public int energyPerJumper() {
        return energyPerJumper.get();
    }

    public int buildTicks() {
        return buildSeconds.get() * Units.TICKS_PER_SECOND;
    }

    public int inputRate() {
        return inputRate.get();
    }

    public int buffer() {
        return buffer.get();
    }
}
