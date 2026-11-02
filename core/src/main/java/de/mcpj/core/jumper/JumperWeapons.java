package de.mcpj.core.jumper;

import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.Vec3d;

/** Drone launchers in the pods: cooldown, energy per launch, alternating sides; firing decloaks. */
public final class JumperWeapons {
    private static final long NEVER = -1_000_000L;
    private static final int PODS_DEPLOY_TICKS = 100;
    private static final double LAUNCH_SPEED = 0.45;
    private static final double LAUNCH_SIDE_SPEED = 0.12;
    private static final double LAUNCH_LIFT = 0.08;

    private final Jumper jumper;
    private long lastLaunchTick = NEVER;
    private boolean leftLauncherNext;

    JumperWeapons(Jumper jumper) {
        this.jumper = jumper;
    }

    public boolean isReady() {
        return jumper.host().gameTime() - lastLaunchTick >= ServerConfig.DRONES.cooldownTicks();
    }

    void fire(Vec3d pilotLook, int targetId) {
        if (!isReady()) return;
        lastLaunchTick = jumper.host().gameTime();
        if (!jumper.energy().tryConsume(ServerConfig.DRONES.energyCost())) {
            jumper.systems().denied();
            return;
        }
        if (jumper.isCloaked()) jumper.systems().setCloak(false);
        jumper.pods().deployFor(PODS_DEPLOY_TICKS);
        launch(pilotLook, targetId);
    }

    private void launch(Vec3d pilotLook, int targetId) {
        leftLauncherNext = !leftLauncherNext;
        int side = leftLauncherNext ? 1 : -1;
        Vec3d spawn = jumper.modelToWorld(side * JumperGeometry.LAUNCHER_X, JumperGeometry.LAUNCHER_Y, JumperGeometry.LAUNCHER_Z);
        Vec3d velocity = pilotLook.scale(LAUNCH_SPEED)
                .add(jumper.left().scale(side * LAUNCH_SIDE_SPEED))
                .add(0, LAUNCH_LIFT, 0)
                .add(jumper.serverVelocity());
        jumper.host().launchDrone(spawn, velocity, targetId);
        JumperSounds.droneLaunched(jumper);
    }
}
