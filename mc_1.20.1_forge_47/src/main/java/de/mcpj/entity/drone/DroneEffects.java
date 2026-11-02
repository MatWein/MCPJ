package de.mcpj.entity.drone;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class DroneEffects {
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(1.0f, 0.78f, 0.25f), 1.1f);
    private static final int TRAIL_PARTICLES_PER_TICK = 2;
    private static final double CENTER_HEIGHT = 0.15;
    private static final double BUBBLE_DRIFT = 0.2;
    private static final double BUBBLE_RISE = 0.05;
    private static final int SPARK_ONE_IN = 3;
    private static final int EXPLOSION_TRAIL_COUNT = 24;
    private static final double EXPLOSION_TRAIL_SPREAD = 0.5;
    private static final double EXPLOSION_TRAIL_SPEED = 0.05;
    private static final int EXPLOSION_SPARK_COUNT = 16;
    private static final double EXPLOSION_SPARK_SPREAD = 0.4;
    private static final double EXPLOSION_SPARK_SPEED = 0.4;
    private static final int FIZZLE_COUNT = 12;
    private static final double FIZZLE_SPREAD = 0.3;
    private static final double FIZZLE_SPEED = 0.03;

    private DroneEffects() {}

    static void trail(DroneEntity drone) {
        Vec3 velocity = drone.getDeltaMovement();
        RandomSource random = drone.getRandom();
        double y = drone.getY() + CENTER_HEIGHT;
        for (int i = 0; i < TRAIL_PARTICLES_PER_TICK; i++) {
            double t = random.nextDouble();
            drone.level().addParticle(TRAIL, drone.getX() - velocity.x * t, y - velocity.y * t, drone.getZ() - velocity.z * t, 0, 0, 0);
        }
        if (drone.isInWater()) {
            drone.level().addParticle(ParticleTypes.BUBBLE, drone.getX(), y, drone.getZ(),
                    -velocity.x * BUBBLE_DRIFT, BUBBLE_RISE, -velocity.z * BUBBLE_DRIFT);
        } else if (random.nextInt(SPARK_ONE_IN) == 0) {
            drone.level().addParticle(ParticleTypes.ELECTRIC_SPARK, drone.getX(), y, drone.getZ(), 0, 0, 0);
        }
    }

    static void explosion(ServerLevel level, Vec3 at) {
        level.sendParticles(TRAIL, at.x, at.y, at.z, EXPLOSION_TRAIL_COUNT,
                EXPLOSION_TRAIL_SPREAD, EXPLOSION_TRAIL_SPREAD, EXPLOSION_TRAIL_SPREAD, EXPLOSION_TRAIL_SPEED);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, EXPLOSION_SPARK_COUNT,
                EXPLOSION_SPARK_SPREAD, EXPLOSION_SPARK_SPREAD, EXPLOSION_SPARK_SPREAD, EXPLOSION_SPARK_SPEED);
    }

    static void fizzle(DroneEntity drone) {
        if (drone.level() instanceof ServerLevel level) {
            level.sendParticles(TRAIL, drone.getX(), drone.getY(), drone.getZ(), FIZZLE_COUNT,
                    FIZZLE_SPREAD, FIZZLE_SPREAD, FIZZLE_SPREAD, FIZZLE_SPEED);
            drone.playFizzleSound();
        }
    }
}
