package de.mcpj.core.targeting;

import de.mcpj.core.math.Box;
import de.mcpj.core.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tab cycling through targets: nearest first, then the nearest not yet selected; after the last one no target;
 * after a pause the cycle starts over. Works with the platform's entity ids.
 */
public final class TargetCycle {
    private static final int CYCLE_RESET_TICKS = 60;
    private static final long NEVER = -1_000_000L;

    private final Set<Integer> visited = new HashSet<>();
    private long lastCycleTick = NEVER;

    /**
     * Next target after {@code current}.
     *
     * @param candidates ids of all valid targets, nearest first
     * @return the id of the new target, or null for none
     */
    @Nullable
    public Integer next(long gameTime, @Nullable Integer current, List<Integer> candidates) {
        if (gameTime - lastCycleTick > CYCLE_RESET_TICKS) visited.clear();
        lastCycleTick = gameTime;
        if (current != null) visited.add(current);
        Integer next = candidates.stream().filter(id -> !visited.contains(id)).findFirst().orElse(null);
        if (next == null) visited.clear();
        return next;
    }

    /** Squared distance from a point to the nearest point of a box (0 inside). */
    public static double distanceSqr(Box box, Vec3d from) {
        double dx = Math.max(0, Math.max(box.minX() - from.x(), from.x() - box.maxX()));
        double dy = Math.max(0, Math.max(box.minY() - from.y(), from.y() - box.maxY()));
        double dz = Math.max(0, Math.max(box.minZ() - from.z(), from.z() - box.maxZ()));
        return dx * dx + dy * dy + dz * dz;
    }
}
