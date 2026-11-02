package de.mcpj.core.config;

import de.mcpj.core.util.Units;

public final class HullConfig {
    private final DoubleSetting maxHull;
    private final DoubleSetting droneHitFraction;
    private final DoubleSetting waterDamagePerSecond;
    private final DoubleSetting lavaDamagePerSecond;
    private final DoubleSetting destructionExplosionPower;
    private final DoubleSetting impactSafeSpeed;
    private final DoubleSetting impactFullSpeed;
    private final DoubleSetting impactSafeFallSpeed;
    private final DoubleSetting impactFullFallSpeed;
    private final DoubleSetting maxImpactFraction;

    HullConfig(ConfigBuilder builder) {
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
        return (float) maxHull.get();
    }

    public float droneHitFraction() {
        return (float) droneHitFraction.get();
    }

    public float waterDamagePerSecond() {
        return (float) waterDamagePerSecond.get();
    }

    public float lavaDamagePerSecond() {
        return (float) lavaDamagePerSecond.get();
    }

    public float destructionExplosionPower() {
        return (float) destructionExplosionPower.get();
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
        return (float) maxImpactFraction.get();
    }
}
