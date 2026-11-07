package de.mcpj.core.navigation;

import de.mcpj.core.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Compass math for the HUD's compass band (degrees, 0 = north, 90 = east). */
public final class Compass {
    /** Distance between two marks of the band. */
    public static final int MARK_STEP = 15;
    private static final int FULL_CIRCLE = 360;
    private static final int HALF_CIRCLE = 180;
    private static final int DIRECTION_STEP = 45;
    /** Minecraft yaw 0 points south. */
    private static final float YAW_TO_HEADING = 180f;
    /** Direction names (translation key suffixes) every 45 degrees, starting north. */
    private static final String[] DIRECTIONS = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};

    /**
     * A mark on the band.
     *
     * @param offset    angle from the current heading (-halfRange..halfRange, positive = right)
     * @param direction direction name every 45 degrees (n, ne, e, ...), else null
     */
    public record Mark(float offset, @Nullable String direction) {}

    private Compass() {}

    /** Heading of the ship from its Minecraft yaw. */
    public static float heading(float yaw) {
        return normalize(yaw + YAW_TO_HEADING);
    }

    /** Direction from {@code from} to {@code to} (north = -z, east = +x). */
    public static float bearing(Vec3d from, Vec3d to) {
        return normalize((float) Math.toDegrees(Math.atan2(to.x() - from.x(), from.z() - to.z())));
    }

    /** Angle from the heading to the bearing, -180..180 (positive = to the right). */
    public static float relative(float bearing, float heading) {
        float relative = normalize(bearing - heading);
        return relative > HALF_CIRCLE ? relative - FULL_CIRCLE : relative;
    }

    /** All marks within halfRange degrees left and right of the heading. */
    public static List<Mark> marks(float heading, float halfRange) {
        List<Mark> marks = new ArrayList<>();
        int first = (int) Math.ceil((heading - halfRange) / MARK_STEP) * MARK_STEP;
        for (int angle = first; angle <= heading + halfRange; angle += MARK_STEP) {
            int absolute = Math.floorMod(angle, FULL_CIRCLE);
            String direction = absolute % DIRECTION_STEP == 0 ? DIRECTIONS[absolute / DIRECTION_STEP] : null;
            marks.add(new Mark(angle - heading, direction));
        }
        return marks;
    }

    private static float normalize(float degrees) {
        float result = degrees % FULL_CIRCLE;
        return result < 0 ? result + FULL_CIRCLE : result;
    }
}
