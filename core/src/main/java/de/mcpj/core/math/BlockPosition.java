package de.mcpj.core.math;

/** Integer block coordinates, the core's counterpart to Minecraft's BlockPos. */
public record BlockPosition(int x, int y, int z) {
    public static BlockPosition containing(Vec3d point) {
        return new BlockPosition((int) Math.floor(point.x()), (int) Math.floor(point.y()), (int) Math.floor(point.z()));
    }

    public Vec3d center() {
        return new Vec3d(x + 0.5, y + 0.5, z + 0.5);
    }
}
