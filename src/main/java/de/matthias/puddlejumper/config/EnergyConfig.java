package de.matthias.puddlejumper.config;

import de.matthias.puddlejumper.util.Units;
import net.minecraftforge.common.ForgeConfigSpec;

public final class EnergyConfig {
    private static final int MAX_DRAIN = 1_000_000;

    private final ForgeConfigSpec.IntValue capacity;
    private final ForgeConfigSpec.DoubleValue startCharge;
    private final ForgeConfigSpec.IntValue hoverDrain;
    private final ForgeConfigSpec.IntValue thrustDrain;
    private final ForgeConfigSpec.DoubleValue waterDriveFactor;
    private final ForgeConfigSpec.IntValue shieldDrain;
    private final ForgeConfigSpec.DoubleValue shieldWaterFactor;
    private final ForgeConfigSpec.IntValue shieldHitCost;
    private final ForgeConfigSpec.IntValue cloakDrain;
    private final ForgeConfigSpec.DoubleValue cloakWaterFactor;
    private final ForgeConfigSpec.DoubleValue underFireFactor;
    private final ForgeConfigSpec.DoubleValue underFireSeconds;
    private final ForgeConfigSpec.IntValue lightDrain;

    EnergyConfig(ForgeConfigSpec.Builder builder) {
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
        shieldDrain = builder.comment("Shield drain (FE/tick)")
                .defineInRange("shieldDrain", 500, 0, MAX_DRAIN);
        shieldWaterFactor = builder.comment("Shield drain multiplier when submerged")
                .defineInRange("shieldWaterFactor", 2.0, 1.0, 100.0);
        shieldHitCost = builder.comment("Extra energy per hull point absorbed by the shield (FE)")
                .defineInRange("shieldHitCost", 200, 0, MAX_DRAIN);
        cloakDrain = builder.comment("Cloak drain (FE/tick)")
                .defineInRange("cloakDrain", 400, 0, MAX_DRAIN);
        cloakWaterFactor = builder.comment("Cloak drain multiplier when submerged")
                .defineInRange("cloakWaterFactor", 1.5, 1.0, 100.0);
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

    public double waterDriveFactor() {
        return waterDriveFactor.get();
    }

    public int shieldDrain() {
        return shieldDrain.get();
    }

    public double shieldWaterFactor() {
        return shieldWaterFactor.get();
    }

    public int shieldHitCost() {
        return shieldHitCost.get();
    }

    public int cloakDrain() {
        return cloakDrain.get();
    }

    public double cloakWaterFactor() {
        return cloakWaterFactor.get();
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
