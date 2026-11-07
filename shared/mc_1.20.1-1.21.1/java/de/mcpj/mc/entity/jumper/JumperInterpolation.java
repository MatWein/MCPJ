package de.mcpj.mc.entity.jumper;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

final class JumperInterpolation {
    private static final int STEPS = 3;

    private final Entity jumper;
    private int stepsLeft;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double targetYaw;

    JumperInterpolation(Entity jumper) {
        this.jumper = jumper;
    }

    void lerpTo(double x, double y, double z, float yaw) {
        targetX = x;
        targetY = y;
        targetZ = z;
        targetYaw = yaw;
        stepsLeft = STEPS;
    }

    void tick() {
        if (jumper.isControlledByLocalInstance()) {
            stepsLeft = 0;
            jumper.syncPacketPositionCodec(jumper.getX(), jumper.getY(), jumper.getZ());
        }
        if (stepsLeft <= 0) return;
        double x = jumper.getX() + (targetX - jumper.getX()) / stepsLeft;
        double y = jumper.getY() + (targetY - jumper.getY()) / stepsLeft;
        double z = jumper.getZ() + (targetZ - jumper.getZ()) / stepsLeft;
        jumper.setYRot(jumper.getYRot() + (float) Mth.wrapDegrees(targetYaw - jumper.getYRot()) / stepsLeft);
        stepsLeft--;
        jumper.setPos(x, y, z);
    }
}
