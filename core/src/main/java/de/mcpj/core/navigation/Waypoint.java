package de.mcpj.core.navigation;

import de.mcpj.core.math.Vec3d;

/** A saved position in the jumper's navigation memory; {@code dimension} is the platform's dimension id. */
public record Waypoint(String name, int x, int y, int z, String dimension) {
    /** Center of the waypoint's block. */
    public Vec3d center() {
        return new Vec3d(x + 0.5, y + 0.5, z + 0.5);
    }
}
