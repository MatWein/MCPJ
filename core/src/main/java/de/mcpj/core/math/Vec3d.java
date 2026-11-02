package de.mcpj.core.math;

/** Immutable 3D vector (world or model space), the core's counterpart to Minecraft's Vec3. */
public record Vec3d(double x, double y, double z) {
    public static final Vec3d ZERO = new Vec3d(0, 0, 0);

    public Vec3d add(Vec3d other) {
        return new Vec3d(x + other.x, y + other.y, z + other.z);
    }

    public Vec3d add(double dx, double dy, double dz) {
        return new Vec3d(x + dx, y + dy, z + dz);
    }

    public Vec3d subtract(Vec3d other) {
        return new Vec3d(x - other.x, y - other.y, z - other.z);
    }

    public Vec3d scale(double factor) {
        return new Vec3d(x * factor, y * factor, z * factor);
    }

    public double dot(Vec3d other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double lengthSqr() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSqr());
    }

    public double distanceTo(Vec3d other) {
        return subtract(other).length();
    }

    /** Unit vector in the same direction; the zero vector stays zero. */
    public Vec3d normalize() {
        double length = length();
        return length < 1.0e-4 ? ZERO : scale(1.0 / length);
    }

    /** Rotation about the vertical axis by {@code radians}, with Minecraft's yaw convention. */
    public Vec3d yRot(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(x * cos + z * sin, y, z * cos - x * sin);
    }

    public Vec3d withX(double newX) {
        return new Vec3d(newX, y, z);
    }

    public Vec3d withY(double newY) {
        return new Vec3d(x, newY, z);
    }

    public Vec3d withZ(double newZ) {
        return new Vec3d(x, y, newZ);
    }
}
