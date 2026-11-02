package de.mcpj.config;

import de.mcpj.util.Units;
import net.minecraftforge.common.ForgeConfigSpec;

public final class DroneConfig {
    private final ForgeConfigSpec.IntValue cooldownTicks;
    private final ForgeConfigSpec.IntValue energyCost;
    private final ForgeConfigSpec.DoubleValue damage;
    private final ForgeConfigSpec.DoubleValue explosionPower;
    private final ForgeConfigSpec.DoubleValue speed;
    private final ForgeConfigSpec.DoubleValue waterSpeedFactor;
    private final ForgeConfigSpec.DoubleValue lifetimeSeconds;
    private final ForgeConfigSpec.DoubleValue retargetRange;

    DroneConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Drone weapons").push("drones");
        cooldownTicks = builder.comment("Ticks between two launches")
                .defineInRange("cooldownTicks", 15, 1, 1200);
        energyCost = builder.comment("Energy per launch (FE)")
                .defineInRange("energyCost", 150_000, 0, Integer.MAX_VALUE);
        damage = builder.comment("Direct hit damage")
                .defineInRange("damage", 8.0, 0.0, 1000.0);
        explosionPower = builder.comment("Explosion power on impact (creeper: 3); breaks blocks only with mobGriefing")
                .defineInRange("explosionPower", 2.0, 0.0, 10.0);
        speed = builder.comment("Top speed in blocks per second")
                .defineInRange("speed", 22.0, 1.0, 100.0);
        waterSpeedFactor = builder.comment("Speed and agility multiplier underwater")
                .defineInRange("waterSpeedFactor", 0.5, 0.0, 1.0);
        lifetimeSeconds = builder.comment("Seconds until a drone fizzles out")
                .defineInRange("lifetimeSeconds", 16.0, 1.0, 120.0);
        retargetRange = builder.comment("Range in which a drone without target looks for a new hostile (blocks)")
                .defineInRange("retargetRange", 28.0, 0.0, 128.0);
        builder.pop();
    }

    public int cooldownTicks() {
        return cooldownTicks.get();
    }

    public int energyCost() {
        return energyCost.get();
    }

    public float damage() {
        return damage.get().floatValue();
    }

    public float explosionPower() {
        return explosionPower.get().floatValue();
    }

    public double speedPerTick() {
        return Units.perTick(speed.get());
    }

    public double waterSpeedFactor() {
        return waterSpeedFactor.get();
    }

    public int lifetimeTicks() {
        return Units.ticks(lifetimeSeconds.get());
    }

    public double retargetRange() {
        return retargetRange.get();
    }
}
