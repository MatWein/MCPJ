package de.mcpj.config;

import de.mcpj.util.Units;
import net.minecraftforge.common.ForgeConfigSpec;

public final class HullConfig {
    private final ForgeConfigSpec.DoubleValue maxHull;
    private final ForgeConfigSpec.DoubleValue droneHitFraction;
    private final ForgeConfigSpec.DoubleValue waterDamagePerSecond;
    private final ForgeConfigSpec.DoubleValue lavaDamagePerSecond;
    private final ForgeConfigSpec.DoubleValue destructionExplosionPower;
    private final ForgeConfigSpec.DoubleValue impactSafeSpeed;
    private final ForgeConfigSpec.DoubleValue impactFullSpeed;
    private final ForgeConfigSpec.DoubleValue impactSafeFallSpeed;
    private final ForgeConfigSpec.DoubleValue impactFullFallSpeed;
    private final ForgeConfigSpec.DoubleValue maxImpactFraction;

    HullConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Hull integrity and damage").push("hull");
        maxHull = builder.comment("Hull points (a diamond sword deals 7 per hit)")
                .defineInRange("maxHull", 240.0, 1.0, 100_000.0);
        droneHitFraction = builder.comment("Share of the hull destroyed by one hostile drone")
                .defineInRange("droneHitFraction", 0.6, 0.0, 1.0);
        waterDamagePerSecond = builder.comment("Hull points lost per second while submerged without shield")
                .defineInRange("waterDamagePerSecond", 1.0, 0.0, 1000.0);
        lavaDamagePerSecond = builder.comment("Hull points lost per second while touching lava (also flowing lava);",
                        "an active shield absorbs it as long as there is energy")
                .defineInRange("lavaDamagePerSecond", 8.0, 0.0, 1000.0);
        destructionExplosionPower = builder.comment("Explosion power when the jumper is destroyed (creeper: 3, TNT: 4);",
                        "breaks blocks only with mobGriefing, 0 = no explosion")
                .defineInRange("destructionExplosionPower", 4.0, 0.0, 10.0);
        impactSafeSpeed = builder.comment("Collisions below this speed (blocks per second) cause no damage")
                .defineInRange("impactSafeSpeed", 8.0, 0.0, 100.0);
        impactFullSpeed = builder.comment("Collisions at this speed (blocks per second) cause maximum damage")
                .defineInRange("impactFullSpeed", 20.0, 0.1, 100.0);
        impactSafeFallSpeed = builder.comment("Landings below this sink rate (blocks per second) cause no damage")
                .defineInRange("impactSafeFallSpeed", 15.0, 0.0, 100.0);
        impactFullFallSpeed = builder.comment("Crashes at this sink rate (blocks per second) cause maximum damage")
                .defineInRange("impactFullFallSpeed", 32.0, 0.1, 100.0);
        maxImpactFraction = builder.comment("Maximum share of the hull lost in a single collision")
                .defineInRange("maxImpactFraction", 0.3, 0.0, 1.0);
        builder.pop();
    }

    public float maxHull() {
        return maxHull.get().floatValue();
    }

    public float droneHitFraction() {
        return droneHitFraction.get().floatValue();
    }

    public float waterDamagePerSecond() {
        return waterDamagePerSecond.get().floatValue();
    }

    public float lavaDamagePerSecond() {
        return lavaDamagePerSecond.get().floatValue();
    }

    public float destructionExplosionPower() {
        return destructionExplosionPower.get().floatValue();
    }

    public double impactSafeSpeedPerTick() {
        return Units.perTick(impactSafeSpeed.get());
    }

    public double impactFullSpeedPerTick() {
        return Units.perTick(impactFullSpeed.get());
    }

    public double impactSafeFallSpeedPerTick() {
        return Units.perTick(impactSafeFallSpeed.get());
    }

    public double impactFullFallSpeedPerTick() {
        return Units.perTick(impactFullFallSpeed.get());
    }

    public float maxImpactFraction() {
        return maxImpactFraction.get().floatValue();
    }
}
