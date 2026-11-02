package de.matthias.puddlejumper.entity.drone;

import de.matthias.puddlejumper.config.DroneConfig;
import de.matthias.puddlejumper.config.ServerConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

final class DroneGuidance {
    private static final int RETARGET_INTERVAL = 5;
    private static final int LAUNCH_PHASE_TICKS = 8;
    private static final double ACCELERATION = 0.035;
    private static final double WATER_ACCELERATION = 0.015;
    private static final double LAUNCH_TURN_RATE = 0.04;
    private static final double CRUISE_TURN_RATE = 0.12;
    private static final double CLOSE_RANGE_TURN_BONUS = 0.35;
    private static final double CLOSE_RANGE = 10.0;
    private static final double WATER_TURN_FACTOR = 0.6;
    private static final double MIN_CORNER_SPEED = 0.75;
    private static final double RETARGET_MIN_ALIGNMENT = 0.2;
    private static final double MIN_DIRECTION_SQR = 1.0e-6;
    private static final Vec3 DEFAULT_DIRECTION = new Vec3(0, 0, 1);

    private final DroneEntity drone;
    @Nullable private Entity target;

    DroneGuidance(DroneEntity drone) {
        this.drone = drone;
    }

    void setTarget(@Nullable Entity target) {
        this.target = target;
    }

    Vec3 steer(int age) {
        Vec3 velocity = drone.getDeltaMovement();
        if (!hasLiveTarget() && age % RETARGET_INTERVAL == 0) target = acquireTarget(velocity);
        DroneConfig config = ServerConfig.DRONES;
        boolean water = drone.isInWater();
        double maxSpeed = config.speedPerTick() * (water ? config.waterSpeedFactor() : 1.0);
        double speed = Math.min(maxSpeed, velocity.length() + (water ? WATER_ACCELERATION : ACCELERATION));
        Vec3 direction = velocity.lengthSqr() > MIN_DIRECTION_SQR ? velocity.normalize() : DEFAULT_DIRECTION;
        if (hasLiveTarget()) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(drone.position());
            Vec3 wanted = toTarget.normalize();
            double closeness = Mth.clamp(1.0 - toTarget.length() / CLOSE_RANGE, 0.0, 1.0);
            double turnRate = (age < LAUNCH_PHASE_TICKS ? LAUNCH_TURN_RATE : CRUISE_TURN_RATE + CLOSE_RANGE_TURN_BONUS * closeness)
                    * (water ? WATER_TURN_FACTOR : 1.0);
            direction = direction.lerp(wanted, turnRate).normalize();
            speed *= MIN_CORNER_SPEED + (1 - MIN_CORNER_SPEED) * Math.max(0, direction.dot(wanted));
        }
        return direction.scale(speed);
    }

    private boolean hasLiveTarget() {
        return target != null && target.isAlive();
    }

    @Nullable
    private Entity acquireTarget(Vec3 velocity) {
        Vec3 direction = velocity.lengthSqr() > MIN_DIRECTION_SQR ? velocity.normalize() : Vec3.ZERO;
        Vec3 origin = drone.position();
        return drone.level().getEntitiesOfClass(LivingEntity.class,
                        drone.getBoundingBox().inflate(ServerConfig.DRONES.retargetRange()),
                        entity -> entity instanceof Enemy && entity.isAlive() && drone.canHitEntity(entity)
                                && entity.position().subtract(origin).normalize().dot(direction) > RETARGET_MIN_ALIGNMENT)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(drone)))
                .orElse(null);
    }
}
