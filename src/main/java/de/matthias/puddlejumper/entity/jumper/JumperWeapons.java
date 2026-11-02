package de.matthias.puddlejumper.entity.jumper;

import de.matthias.puddlejumper.config.ServerConfig;
import de.matthias.puddlejumper.entity.drone.DroneEntity;
import de.matthias.puddlejumper.network.JumperActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public final class JumperWeapons {
    private static final long NEVER = -1_000_000L;
    private static final int PODS_DEPLOY_TICKS = 100;
    private static final double LAUNCH_SPEED = 0.45;
    private static final double LAUNCH_SIDE_SPEED = 0.12;
    private static final double LAUNCH_LIFT = 0.08;
    private static final double AUTO_TARGET_MIN_ALIGNMENT = 0.3;

    private final PuddleJumperEntity jumper;
    private long lastLaunchTick = NEVER;
    private boolean leftLauncherNext;

    JumperWeapons(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    public boolean isReady() {
        return jumper.level().getGameTime() - lastLaunchTick >= ServerConfig.DRONES.cooldownTicks();
    }

    void fire(ServerPlayer pilot, int targetId) {
        if (!isReady()) return;
        lastLaunchTick = jumper.level().getGameTime();
        if (!jumper.energy().tryConsume(ServerConfig.DRONES.energyCost())) {
            jumper.systems().denied();
            return;
        }
        if (jumper.isCloaked()) jumper.systems().setCloak(false);
        jumper.pods().deployFor(PODS_DEPLOY_TICKS);
        launch(pilot, resolveTarget(targetId));
    }

    private void launch(ServerPlayer pilot, @Nullable Entity target) {
        leftLauncherNext = !leftLauncherNext;
        int side = leftLauncherNext ? 1 : -1;
        Vec3 spawn = jumper.modelToWorld(side * JumperGeometry.LAUNCHER_X, JumperGeometry.LAUNCHER_Y, JumperGeometry.LAUNCHER_Z);
        Vec3 velocity = pilot.getLookAngle().scale(LAUNCH_SPEED)
                .add(jumper.left().scale(side * LAUNCH_SIDE_SPEED))
                .add(0, LAUNCH_LIFT, 0)
                .add(jumper.serverVelocity());

        DroneEntity drone = new DroneEntity(jumper.level(), pilot, jumper, target);
        drone.setPos(spawn);
        drone.setDeltaMovement(velocity);
        jumper.level().addFreshEntity(drone);
        jumper.level().playSound(null, spawn.x, spawn.y, spawn.z, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.8f, 1.8f);
        jumper.level().playSound(null, spawn.x, spawn.y, spawn.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 0.6f);
    }

    @Nullable
    private Entity resolveTarget(int targetId) {
        Entity selected = targetId == JumperActionPacket.NO_TARGET ? null : jumper.level().getEntity(targetId);
        double lockRange = ServerConfig.TARGETING.lockRange();
        if (jumper.isValidTarget(selected) && selected.distanceToSqr(jumper) <= lockRange * lockRange) return selected;
        return findAutoTarget();
    }

    @Nullable
    private LivingEntity findAutoTarget() {
        Vec3 forward = jumper.forward();
        Vec3 center = jumper.position();
        return jumper.level().getEntitiesOfClass(LivingEntity.class,
                        jumper.getBoundingBox().inflate(ServerConfig.TARGETING.autoTargetRange()),
                        entity -> entity instanceof Enemy && entity.isAlive() && !jumper.hasPassenger(entity)
                                && entity.position().subtract(center).normalize().dot(forward) > AUTO_TARGET_MIN_ALIGNMENT)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(jumper)))
                .orElse(null);
    }
}
