package de.mcpj.core.config;

import de.mcpj.core.jumper.JumperMedium;
import de.mcpj.core.util.Units;

public final class EnergyConfig {
    private static final int MAX_DRAIN = 1_000_000;

    private final IntSetting capacity;
    private final DoubleSetting startCharge;
    private final IntSetting hoverDrain;
    private final IntSetting thrustDrain;
    private final DoubleSetting waterDriveFactor;
    private final DoubleSetting lavaDriveFactor;
    private final IntSetting shieldDrain;
    private final DoubleSetting shieldWaterFactor;
    private final DoubleSetting shieldLavaFactor;
    private final IntSetting shieldHitCost;
    private final IntSetting cloakDrain;
    private final DoubleSetting cloakWaterFactor;
    private final DoubleSetting cloakLavaFactor;
    private final DoubleSetting underFireFactor;
    private final DoubleSetting underFireSeconds;
    private final IntSetting lightDrain;

    EnergyConfig(ConfigBuilder builder) {
        builder.comment("Energy of the Puddle Jumper in Forge Energy (FE). Mekanism converts automatically (1 FE = 2.5 J).")
                .push("energy");
        capacity = builder.comment("Storage capacity")
                .defineInRange("capacity", 60_000_000, 1_000, Integer.MAX_VALUE);
        startCharge = builder.comment("Charge of newly placed jumpers (0..1)")
                .defineInRange("startCharge", 0.2, 0.0, 1.0);
        hoverDrain = builder.comment("Drive drain while airborne and hovering (FE/tick); landed: 0")
                .defineInRange("hoverDrain", 250, 0, MAX_DRAIN);
        thrustDrain = builder.comment("Drive drain at top speed (FE/tick), interpolated linearly from hoverDrain")
                .defineInRange("thrustDrain", 1_500, 0, MAX_DRAIN);
        waterDriveFactor = builder.comment("Drive drain multiplier when submerged")
                .defineInRange("waterDriveFactor", 1.5, 1.0, 100.0);
        lavaDriveFactor = builder.comment("Drive drain multiplier when touching lava")
                .defineInRange("lavaDriveFactor", 2.0, 1.0, 100.0);
        shieldDrain = builder.comment("Shield drain (FE/tick)")
                .defineInRange("shieldDrain", 500, 0, MAX_DRAIN);
        shieldWaterFactor = builder.comment("Shield drain multiplier when submerged")
                .defineInRange("shieldWaterFactor", 2.0, 1.0, 100.0);
        shieldLavaFactor = builder.comment("Shield drain multiplier when touching lava")
                .defineInRange("shieldLavaFactor", 3.0, 1.0, 100.0);
        shieldHitCost = builder.comment("Extra energy per hull point absorbed by the shield (FE)")
                .defineInRange("shieldHitCost", 200, 0, MAX_DRAIN);
        cloakDrain = builder.comment("Cloak drain (FE/tick)")
                .defineInRange("cloakDrain", 400, 0, MAX_DRAIN);
        cloakWaterFactor = builder.comment("Cloak drain multiplier when submerged")
                .defineInRange("cloakWaterFactor", 1.5, 1.0, 100.0);
        cloakLavaFactor = builder.comment("Cloak drain multiplier when touching lava")
                .defineInRange("cloakLavaFactor", 2.0, 1.0, 100.0);
        underFireFactor = builder.comment("Shield and cloak drain multiplier while under fire")
                .defineInRange("underFireFactor", 2.0, 1.0, 100.0);
        underFireSeconds = builder.comment("How long the jumper counts as under fire after a hit (seconds)")
                .defineInRange("underFireSeconds", 2.0, 0.0, 60.0);
        lightDrain = builder.comment("Headlight drain (FE/tick)")
                .defineInRange("lightDrain", 20, 0, MAX_DRAIN);
        builder.pop();
    }

    public int capacity() {
        return capacity.get();
    }

    public double startCharge() {
        return startCharge.get();
    }

    public int hoverDrain() {
        return hoverDrain.get();
    }

    public int thrustDrain() {
        return thrustDrain.get();
    }

    public double driveFactor(JumperMedium medium) {
        return switch (medium) {
            case AIR -> 1.0;
            case WATER -> waterDriveFactor.get();
            case LAVA -> lavaDriveFactor.get();
        };
    }

    public int shieldDrain() {
        return shieldDrain.get();
    }

    public double shieldFactor(JumperMedium medium) {
        return switch (medium) {
            case AIR -> 1.0;
            case WATER -> shieldWaterFactor.get();
            case LAVA -> shieldLavaFactor.get();
        };
    }

    public int shieldHitCost() {
        return shieldHitCost.get();
    }

    public int cloakDrain() {
        return cloakDrain.get();
    }

    public double cloakFactor(JumperMedium medium) {
        return switch (medium) {
            case AIR -> 1.0;
            case WATER -> cloakWaterFactor.get();
            case LAVA -> cloakLavaFactor.get();
        };
    }

    public double underFireFactor() {
        return underFireFactor.get();
    }

    public int underFireTicks() {
        return Units.ticks(underFireSeconds.get());
    }

    public int lightDrain() {
        return lightDrain.get();
    }
}
