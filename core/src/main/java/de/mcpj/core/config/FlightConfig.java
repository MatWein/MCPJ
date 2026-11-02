package de.mcpj.core.config;

import de.mcpj.core.jumper.JumperMedium;
import de.mcpj.core.util.Units;

public final class FlightConfig {
    private final DoubleSetting maxSpeed;
    private final DoubleSetting acceleration;
    private final DoubleSetting reverseFactor;
    private final DoubleSetting strafeFactor;
    private final DoubleSetting verticalFactor;
    private final DoubleSetting turnAcceleration;
    private final DoubleSetting thrustDrag;
    private final DoubleSetting idleDrag;
    private final DoubleSetting turnDrag;
    private final DoubleSetting waterAccelerationFactor;
    private final DoubleSetting waterTurnFactor;
    private final DoubleSetting waterThrustDrag;
    private final DoubleSetting waterIdleDrag;
    private final DoubleSetting lavaAccelerationFactor;
    private final DoubleSetting lavaTurnFactor;
    private final DoubleSetting lavaThrustDrag;
    private final DoubleSetting lavaIdleDrag;

    FlightConfig(ConfigBuilder builder) {
        builder.comment("Flight model of the Puddle Jumper").push("flight");
        maxSpeed = builder.comment("Top speed in blocks per second")
                .defineInRange("maxSpeed", 20.0, 1.0, 100.0);
        acceleration = builder.comment("Forward thrust in blocks per second squared")
                .defineInRange("acceleration", 8.8, 0.1, 100.0);
        reverseFactor = builder.comment("Reverse thrust relative to forward thrust")
                .defineInRange("reverseFactor", 0.6, 0.0, 1.0);
        strafeFactor = builder.comment("Sideways thrust relative to forward thrust")
                .defineInRange("strafeFactor", 0.7, 0.0, 1.0);
        verticalFactor = builder.comment("Climb/descend thrust relative to forward thrust")
                .defineInRange("verticalFactor", 0.8, 0.0, 1.0);
        turnAcceleration = builder.comment("Yaw acceleration in degrees per second squared")
                .defineInRange("turnAcceleration", 120.0, 1.0, 2000.0);
        thrustDrag = builder.comment("Velocity kept per tick while thrusting (lower = more air resistance)")
                .defineInRange("thrustDrag", 0.975, 0.5, 1.0);
        idleDrag = builder.comment("Velocity kept per tick without input (lower = stops faster)")
                .defineInRange("idleDrag", 0.95, 0.5, 1.0);
        turnDrag = builder.comment("Yaw rate kept per tick")
                .defineInRange("turnDrag", 0.88, 0.5, 1.0);
        waterAccelerationFactor = builder.comment("Thrust multiplier when submerged")
                .defineInRange("waterAccelerationFactor", 0.5, 0.0, 1.0);
        waterTurnFactor = builder.comment("Turn multiplier when submerged")
                .defineInRange("waterTurnFactor", 0.5, 0.0, 1.0);
        waterThrustDrag = builder.comment("Velocity kept per tick while thrusting underwater")
                .defineInRange("waterThrustDrag", 0.955, 0.5, 1.0);
        waterIdleDrag = builder.comment("Velocity kept per tick without input underwater")
                .defineInRange("waterIdleDrag", 0.92, 0.5, 1.0);
        lavaAccelerationFactor = builder.comment("Thrust multiplier when touching lava")
                .defineInRange("lavaAccelerationFactor", 0.3, 0.0, 1.0);
        lavaTurnFactor = builder.comment("Turn multiplier when touching lava")
                .defineInRange("lavaTurnFactor", 0.3, 0.0, 1.0);
        lavaThrustDrag = builder.comment("Velocity kept per tick while thrusting in lava")
                .defineInRange("lavaThrustDrag", 0.92, 0.5, 1.0);
        lavaIdleDrag = builder.comment("Velocity kept per tick without input in lava")
                .defineInRange("lavaIdleDrag", 0.85, 0.5, 1.0);
        builder.pop();
    }

    public double maxSpeedPerTick() {
        return Units.perTick(maxSpeed.get());
    }

    public double accelerationPerTick() {
        return Units.perTickSquared(acceleration.get());
    }

    public double reverseFactor() {
        return reverseFactor.get();
    }

    public double strafeFactor() {
        return strafeFactor.get();
    }

    public double verticalFactor() {
        return verticalFactor.get();
    }

    public float turnAccelerationPerTick() {
        return (float) Units.perTickSquared(turnAcceleration.get());
    }

    public double thrustDrag(JumperMedium medium) {
        return switch (medium) {
            case AIR -> thrustDrag.get();
            case WATER -> waterThrustDrag.get();
            case LAVA -> lavaThrustDrag.get();
        };
    }

    public double idleDrag(JumperMedium medium) {
        return switch (medium) {
            case AIR -> idleDrag.get();
            case WATER -> waterIdleDrag.get();
            case LAVA -> lavaIdleDrag.get();
        };
    }

    public float turnDrag() {
        return (float) turnDrag.get();
    }

    public double accelerationFactor(JumperMedium medium) {
        return switch (medium) {
            case AIR -> 1.0;
            case WATER -> waterAccelerationFactor.get();
            case LAVA -> lavaAccelerationFactor.get();
        };
    }

    public float turnFactor(JumperMedium medium) {
        return switch (medium) {
            case AIR -> 1f;
            case WATER -> (float) waterTurnFactor.get();
            case LAVA -> (float) lavaTurnFactor.get();
        };
    }
}
