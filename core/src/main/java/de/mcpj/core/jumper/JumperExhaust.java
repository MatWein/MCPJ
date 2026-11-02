package de.mcpj.core.jumper;

import de.mcpj.core.math.Vec3d;

import java.util.random.RandomGenerator;

/** Client: ion particles and sparks behind the drive pod nozzles while the engines glow. */
final class JumperExhaust {
    private static final float MIN_GLOW = 0.2f;
    private static final float SPAWN_CHANCE_PER_GLOW = 0.45f;
    private static final float SPARK_CHANCE_PER_GLOW = 0.06f;
    private static final double BASE_EJECT_SPEED = 0.04;
    private static final double EJECT_SPEED_PER_SPEED = 0.15;
    private static final double INHERITED_VELOCITY = 0.3;
    private static final double SPARK_SPEED_FACTOR = 4;
    private static final double SPARK_VERTICAL_JITTER = 0.1;
    private static final double NOZZLE_HEIGHT_JITTER = 1.4;
    private static final double NOZZLE_DEPTH = 0.4;

    private final Jumper jumper;

    JumperExhaust(Jumper jumper) {
        this.jumper = jumper;
    }

    void spawn(float glow, float podExtension, Vec3d velocity) {
        if (jumper.isCloaked() || glow <= MIN_GLOW) return;
        RandomGenerator random = jumper.host().random();
        Vec3d eject = jumper.forward().scale(-BASE_EJECT_SPEED - velocity.length() * EJECT_SPEED_PER_SPEED);
        Vec3d particleVelocity = eject.add(velocity.scale(INHERITED_VELOCITY));
        float podX = JumperGeometry.POD_X + JumperGeometry.POD_EXTENSION * podExtension;
        float podY = JumperGeometry.POD_Y - JumperGeometry.POD_DROP * podExtension;
        for (int side = -1; side <= 1; side += 2) {
            for (float nozzle : new float[]{-JumperGeometry.NOZZLE_OFFSET_X, JumperGeometry.NOZZLE_OFFSET_X}) {
                if (random.nextFloat() > glow * SPAWN_CHANCE_PER_GLOW) continue;
                Vec3d at = jumper.modelToWorld(side * podX + nozzle, podY + (random.nextFloat() - 0.5f) * NOZZLE_HEIGHT_JITTER,
                        JumperGeometry.POD_REAR_Z - NOZZLE_DEPTH);
                jumper.host().clientParticle(JumperParticle.ION, at, particleVelocity);
                if (random.nextFloat() < SPARK_CHANCE_PER_GLOW * glow) {
                    jumper.host().clientParticle(JumperParticle.SPARK, at, new Vec3d(eject.x() * SPARK_SPEED_FACTOR,
                            (random.nextFloat() - 0.5f) * SPARK_VERTICAL_JITTER, eject.z() * SPARK_SPEED_FACTOR));
                }
            }
        }
    }
}
