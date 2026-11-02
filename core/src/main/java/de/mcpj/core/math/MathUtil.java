package de.mcpj.core.math;

/** Small numeric helpers (the core's counterpart to Minecraft's Mth). */
public final class MathUtil {
    public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private MathUtil() {}

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float lerp(float t, float from, float to) {
        return from + t * (to - from);
    }

    public static double lerp(double t, double from, double to) {
        return from + t * (to - from);
    }

    /** Angle in degrees wrapped to [-180, 180). */
    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360f;
        if (wrapped >= 180f) wrapped -= 360f;
        if (wrapped < -180f) wrapped += 360f;
        return wrapped;
    }

    public static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) wrapped -= 360.0;
        if (wrapped < -180.0) wrapped += 360.0;
        return wrapped;
    }

    /** Moves {@code current} towards {@code target} by at most {@code step}. */
    public static float approach(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    /** Forward direction (horizontal unit vector) for a Minecraft yaw in degrees. */
    public static Vec3d forward(float yawDegrees) {
        float yaw = yawDegrees * DEG_TO_RAD;
        return new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    /** Left direction (horizontal unit vector) for a Minecraft yaw in degrees. */
    public static Vec3d left(float yawDegrees) {
        float yaw = yawDegrees * DEG_TO_RAD;
        return new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
    }
}
