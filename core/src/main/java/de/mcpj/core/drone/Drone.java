package de.mcpj.core.drone;

import de.mcpj.core.config.DroneConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.MathUtil;
import de.mcpj.core.math.Vec3d;
import de.mcpj.core.util.SaveData;

import java.util.random.RandomGenerator;

/**
 * Homing drone: accelerates, turns gently at launch and sharply near its target, slower underwater,
 * picks a new hostile ahead when it has none, fizzles out after its lifetime.
 */
public final class Drone {
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
    private static final Vec3d DEFAULT_DIRECTION = new Vec3d(0, 0, 1);
    private static final double KNOCKBACK = 0.6;
    private static final double KNOCKBACK_LIFT = 0.25;

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
    private static final String LIFE_TAG = "Life";

    private final DroneHost host;
    private int age;

    public Drone(DroneHost host) {
        this.host = host;
    }

    /** Server: ages the drone; false if it fizzled out (the platform removes it). */
    public boolean serverTick() {
        if (++age > ServerConfig.DRONES.lifetimeTicks()) {
            fizzle();
            return false;
        }
        return true;
    }

    /** Server: the velocity for this tick. */
    public Vec3d steer() {
        Vec3d velocity = host.velocity();
        Vec3d target = host.liveTargetCenter();
        if (target == null && age % RETARGET_INTERVAL == 0) {
            host.acquireTarget(velocity.lengthSqr() > MIN_DIRECTION_SQR ? velocity.normalize() : Vec3d.ZERO,
                    ServerConfig.DRONES.retargetRange(), RETARGET_MIN_ALIGNMENT);
            target = host.liveTargetCenter();
        }
        DroneConfig config = ServerConfig.DRONES;
        boolean water = host.inWater();
        double maxSpeed = config.speedPerTick() * (water ? config.waterSpeedFactor() : 1.0);
        double speed = Math.min(maxSpeed, velocity.length() + (water ? WATER_ACCELERATION : ACCELERATION));
        Vec3d direction = velocity.lengthSqr() > MIN_DIRECTION_SQR ? velocity.normalize() : DEFAULT_DIRECTION;
        if (target != null) {
            Vec3d toTarget = target.subtract(host.location());
            Vec3d wanted = toTarget.normalize();
            double closeness = MathUtil.clamp(1.0 - toTarget.length() / CLOSE_RANGE, 0.0, 1.0);
            double turnRate = (age < LAUNCH_PHASE_TICKS ? LAUNCH_TURN_RATE : CRUISE_TURN_RATE + CLOSE_RANGE_TURN_BONUS * closeness)
                    * (water ? WATER_TURN_FACTOR : 1.0);
            direction = direction.add(wanted.subtract(direction).scale(turnRate)).normalize();
            speed *= MIN_CORNER_SPEED + (1 - MIN_CORNER_SPEED) * Math.max(0, direction.dot(wanted));
        }
        return direction.scale(speed);
    }

    /** Push for an entity the drone hits (x and z; y is {@link #knockbackLift()}). */
    public static Vec3d knockback(Vec3d droneVelocity) {
        return droneVelocity.normalize().scale(KNOCKBACK);
    }

    public static double knockbackLift() {
        return KNOCKBACK_LIFT;
    }

    /** Client: the glowing trail, bubbles underwater, occasional sparks. */
    public void clientTick() {
        Vec3d velocity = host.velocity();
        RandomGenerator random = host.random();
        Vec3d center = host.location().add(0, CENTER_HEIGHT, 0);
        for (int i = 0; i < TRAIL_PARTICLES_PER_TICK; i++) {
            host.clientParticle(DroneParticle.TRAIL, center.subtract(velocity.scale(random.nextDouble())), Vec3d.ZERO);
        }
        if (host.inWater()) {
            host.clientParticle(DroneParticle.BUBBLE, center, new Vec3d(-velocity.x() * BUBBLE_DRIFT, BUBBLE_RISE, -velocity.z() * BUBBLE_DRIFT));
        } else if (random.nextInt(SPARK_ONE_IN) == 0) {
            host.clientParticle(DroneParticle.SPARK, center, Vec3d.ZERO);
        }
    }

    /** Server: the burst of light when the drone explodes at {@code at}. */
    public void explosionEffects(Vec3d at) {
        host.serverParticles(DroneParticle.TRAIL, at, EXPLOSION_TRAIL_COUNT, EXPLOSION_TRAIL_SPREAD, EXPLOSION_TRAIL_SPEED);
        host.serverParticles(DroneParticle.SPARK, at, EXPLOSION_SPARK_COUNT, EXPLOSION_SPARK_SPREAD, EXPLOSION_SPARK_SPEED);
    }

    private void fizzle() {
        host.serverParticles(DroneParticle.TRAIL, host.location(), FIZZLE_COUNT, FIZZLE_SPREAD, FIZZLE_SPEED);
        host.playFizzleSound();
    }

    public void load(SaveData data) {
        age = data.getInt(LIFE_TAG);
    }

    public void save(SaveData data) {
        data.putInt(LIFE_TAG, age);
    }
}
