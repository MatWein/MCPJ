package de.mcpj.core.math;

/** Axis-aligned box in world space, the core's counterpart to Minecraft's AABB. */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    /** Box with the given horizontal footprint around a center point, from {@code bottom} upwards. */
    public static Box around(Vec3d bottomCenter, double halfWidth, double height) {
        return new Box(bottomCenter.x() - halfWidth, bottomCenter.y(), bottomCenter.z() - halfWidth,
                bottomCenter.x() + halfWidth, bottomCenter.y() + height, bottomCenter.z() + halfWidth);
    }

    public Box union(Box other) {
        return new Box(Math.min(minX, other.minX), Math.min(minY, other.minY), Math.min(minZ, other.minZ),
                Math.max(maxX, other.maxX), Math.max(maxY, other.maxY), Math.max(maxZ, other.maxZ));
    }

    public Box inflate(double horizontal, double vertical) {
        return new Box(minX - horizontal, minY - vertical, minZ - horizontal, maxX + horizontal, maxY + vertical, maxZ + horizontal);
    }

    public Box inflate(double amount) {
        return inflate(amount, amount);
    }

    public Box move(Vec3d offset) {
        return new Box(minX + offset.x(), minY + offset.y(), minZ + offset.z(), maxX + offset.x(), maxY + offset.y(), maxZ + offset.z());
    }

    public Vec3d center() {
        return new Vec3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }
}
