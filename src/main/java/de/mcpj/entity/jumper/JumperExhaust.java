package de.mcpj.entity.jumper;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class JumperExhaust {
    private static final DustParticleOptions ION = new DustParticleOptions(new Vector3f(0.45f, 0.75f, 1.0f), 0.6f);
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

    private final PuddleJumperEntity jumper;

    JumperExhaust(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    void spawn(float glow, float podExtension, Vec3 velocity) {
        if (jumper.isCloaked() || glow <= MIN_GLOW) return;
        RandomSource random = jumper.getRandom();
        Vec3 eject = jumper.forward().scale(-BASE_EJECT_SPEED - velocity.length() * EJECT_SPEED_PER_SPEED);
        Vec3 particleVelocity = eject.add(velocity.scale(INHERITED_VELOCITY));
        float podX = JumperGeometry.POD_X + JumperGeometry.POD_EXTENSION * podExtension;
        float podY = JumperGeometry.POD_Y - JumperGeometry.POD_DROP * podExtension;
        for (int side = -1; side <= 1; side += 2) {
            for (float nozzle : new float[]{-JumperGeometry.NOZZLE_OFFSET_X, JumperGeometry.NOZZLE_OFFSET_X}) {
                if (random.nextFloat() > glow * SPAWN_CHANCE_PER_GLOW) continue;
                Vec3 at = jumper.modelToWorld(side * podX + nozzle, podY + (random.nextFloat() - 0.5f) * NOZZLE_HEIGHT_JITTER,
                        JumperGeometry.POD_REAR_Z - NOZZLE_DEPTH);
                jumper.level().addParticle(ION, at.x, at.y, at.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
                if (random.nextFloat() < SPARK_CHANCE_PER_GLOW * glow) {
                    jumper.level().addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, eject.x * SPARK_SPEED_FACTOR,
                            (random.nextFloat() - 0.5f) * SPARK_VERTICAL_JITTER, eject.z * SPARK_SPEED_FACTOR);
                }
            }
        }
    }
}
