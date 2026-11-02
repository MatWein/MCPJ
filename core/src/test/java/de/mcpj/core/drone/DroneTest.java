package de.mcpj.core.drone;

import de.mcpj.core.config.DefaultConfigBuilder;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroneTest {
    private static class Host implements DroneHost {
        Vec3d location = Vec3d.ZERO;
        Vec3d velocity = new Vec3d(0, 0, 0.5);
        @Nullable Vec3d target;
        boolean fizzled;

        @Override public Vec3d location() { return location; }
        @Override public Vec3d velocity() { return velocity; }
        @Override public boolean inWater() { return false; }
        @Override public RandomGenerator random() { return new Random(1); }
        @Override public @Nullable Vec3d liveTargetCenter() { return target; }
        @Override public void acquireTarget(Vec3d direction, double range, double minAlignment) {}
        @Override public void clientParticle(DroneParticle particle, Vec3d at, Vec3d velocity) {}
        @Override public void serverParticles(DroneParticle particle, Vec3d at, int count, double spread, double speed) {}
        @Override public void playFizzleSound() { fizzled = true; }
    }

    @BeforeEach
    void setUp() {
        DefaultConfigBuilder.useDefaults();
    }

    @Test
    void turnsTowardsItsTargetAndKeepsAccelerating() {
        Host host = new Host();
        host.target = new Vec3d(20, 0, 0);
        Drone drone = new Drone(host);
        for (int i = 0; i < 40; i++) {
            drone.serverTick();
            host.velocity = drone.steer();
            host.location = host.location.add(host.velocity);
        }
        assertTrue(host.velocity.x() > host.velocity.z(), "drone did not turn towards the target");
        assertTrue(host.velocity.length() <= ServerConfig.DRONES.speedPerTick() + 1e-9, "faster than its top speed");
    }

    @Test
    void fizzlesOutAfterItsLifetime() {
        Host host = new Host();
        Drone drone = new Drone(host);
        for (int i = 0; i < ServerConfig.DRONES.lifetimeTicks(); i++) assertTrue(drone.serverTick());
        assertFalse(drone.serverTick());
        assertTrue(host.fizzled);
    }
}
