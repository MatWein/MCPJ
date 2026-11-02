package de.mcpj.core.render;

import de.mcpj.core.math.Vec3d;

/** How a drone is drawn: facing its flight direction, spinning, with a pulsing glow. */
public final class DroneLook {
    public static final double CENTER_HEIGHT = 0.25;
    public static final float SCALE = 0.42f;
    private static final float SPIN_DEGREES_PER_TICK = 25f;
    private static final float PULSE_BASE = 0.7f;
    private static final float PULSE_AMPLITUDE = 0.3f;
    private static final float PULSE_SPEED = 0.9f;
    private static final double MIN_VELOCITY_SQR = 1.0e-6;

    /** Apply as rotation about Y by {@code yaw}, about X by {@code pitch} (radians), then about Z by {@code spin} (degrees). */
    public record Orientation(float yaw, float pitch, float spinDegrees) {}

    private DroneLook() {}

    /** {@code moved}: position change since the last tick; {@code velocity} is used while it barely moved. */
    public static Orientation orientation(Vec3d moved, Vec3d velocity, float time) {
        Vec3d direction = moved.lengthSqr() < MIN_VELOCITY_SQR ? velocity : moved;
        double horizontal = Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z());
        return new Orientation((float) Math.atan2(direction.x(), direction.z()), (float) -Math.atan2(direction.y(), horizontal),
                time * SPIN_DEGREES_PER_TICK);
    }

    public static float glowPulse(float time) {
        return PULSE_BASE + PULSE_AMPLITUDE * (float) Math.sin(time * PULSE_SPEED);
    }
}
