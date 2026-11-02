package de.matthias.puddlejumper.entity.jumper;

import de.matthias.puddlejumper.config.FlightConfig;
import de.matthias.puddlejumper.config.ServerConfig;
import de.matthias.puddlejumper.network.JumperImpactPacket;
import de.matthias.puddlejumper.network.ModNetwork;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class JumperFlight {
    public static final double MAX_FALL_SPEED = 3.0;
    private static final double GRAVITY = 0.08;
    private static final double FALL_DRAG = 0.98;
    private static final double WATER_GRAVITY = 0.02;
    private static final double WATER_FALL_DRAG = 0.9;
    private static final double LAVA_GRAVITY = 0.015;
    private static final double LAVA_FALL_DRAG = 0.8;
    private static final double GROUND_FRICTION = 0.6;
    private static final double REST_SPEED_SQR = 1.0e-5;
    private static final float TURN_REST_SPEED = 0.02f;
    private static final double MIN_MOVE_SQR = 1.0e-8;
    private static final double BLOCKED_EPSILON = 1.0e-4;
    private static final double GROUND_PROBE_INSET = 0.05;
    private static final double GROUND_PROBE_DEPTH = 0.08;
    private static final double GROUND_PROBE_OVERLAP = 0.01;

    private final PuddleJumperEntity jumper;
    private float yawVelocity;
    private Vec3 ownVelocity = Vec3.ZERO;

    JumperFlight(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    void tick() {
        // External impulses (explosions, hits, players) are discarded: only our own velocity counts.
        jumper.setDeltaMovement(ownVelocity);
        if (!jumper.isControlledByLocalInstance()) {
            jumper.setDeltaMovement(Vec3.ZERO);
            ownVelocity = Vec3.ZERO;
            return;
        }
        if (jumper.getControllingPassenger() instanceof Player) steer(FlightInput.source.get());
        else drift();
        moveWithSegments(jumper.getDeltaMovement());
        ownVelocity = jumper.getDeltaMovement();
    }

    private void steer(FlightInput input) {
        if (!jumper.energy().hasEnergy()) {
            fall();
            return;
        }
        FlightConfig config = ServerConfig.FLIGHT;
        JumperMedium medium = jumper.medium();
        double acceleration = config.accelerationPerTick() * config.accelerationFactor(medium);

        yawVelocity = yawVelocity * config.turnDrag() - input.turn() * config.turnAccelerationPerTick() * config.turnFactor(medium);
        if (input.turn() == 0 && Math.abs(yawVelocity) < TURN_REST_SPEED) yawVelocity = 0;
        jumper.setYRot(jumper.getYRot() + yawVelocity);

        double forwardAcceleration = input.forward() > 0 ? acceleration : acceleration * config.reverseFactor();
        Vec3 thrust = jumper.forward().scale(input.forward() * forwardAcceleration)
                .add(jumper.left().scale(input.strafe() * acceleration * config.strafeFactor()))
                .add(0, input.vertical() * acceleration * config.verticalFactor(), 0);

        double drag = input.hasThrust() ? config.thrustDrag(medium) : config.idleDrag(medium);
        Vec3 velocity = jumper.getDeltaMovement().scale(drag).add(thrust);
        double maxSpeed = config.maxSpeedPerTick();
        if (velocity.lengthSqr() > maxSpeed * maxSpeed) velocity = velocity.normalize().scale(maxSpeed);
        if (!input.hasThrust() && velocity.lengthSqr() < REST_SPEED_SQR) velocity = Vec3.ZERO;
        jumper.setDeltaMovement(velocity);
    }

    private void drift() {
        if (!jumper.energy().hasEnergy() && !jumper.onGround()) {
            fall();
            return;
        }
        double drag = jumper.onGround() ? GROUND_FRICTION : ServerConfig.FLIGHT.idleDrag(jumper.medium());
        Vec3 velocity = jumper.getDeltaMovement().scale(drag);
        jumper.setDeltaMovement(velocity.lengthSqr() < REST_SPEED_SQR ? Vec3.ZERO : velocity);
    }

    private void fall() {
        Vec3 velocity = jumper.getDeltaMovement();
        JumperMedium medium = jumper.medium();
        if (medium == JumperMedium.LAVA) {
            velocity = velocity.scale(LAVA_FALL_DRAG).add(0, -LAVA_GRAVITY, 0);
        } else if (medium == JumperMedium.WATER) {
            velocity = velocity.scale(WATER_FALL_DRAG).add(0, -WATER_GRAVITY, 0);
        } else {
            double horizontalDrag = jumper.onGround() ? GROUND_FRICTION : FALL_DRAG;
            velocity = new Vec3(velocity.x * horizontalDrag, velocity.y * FALL_DRAG - GRAVITY, velocity.z * horizontalDrag);
        }
        jumper.setDeltaMovement(velocity.x, Math.max(velocity.y, -MAX_FALL_SPEED), velocity.z);
    }

    private void moveWithSegments(Vec3 wanted) {
        if (wanted.lengthSqr() < MIN_MOVE_SQR) {
            jumper.move(MoverType.SELF, Vec3.ZERO);
            return;
        }
        Vec3 allowed = wanted;
        for (double offset : new double[]{JumperGeometry.SEGMENT_OFFSET, -JumperGeometry.SEGMENT_OFFSET}) {
            Vec3 collided = Entity.collideBoundingBox(jumper, allowed, jumper.segmentBox(offset), jumper.level(), List.of());
            allowed = new Vec3(smallerMagnitude(allowed.x, collided.x), smallerMagnitude(allowed.y, collided.y),
                    smallerMagnitude(allowed.z, collided.z));
        }
        Vec3 before = jumper.position();
        jumper.move(MoverType.SELF, allowed);
        Vec3 moved = jumper.position().subtract(before);
        Vec3 velocity = jumper.getDeltaMovement();
        jumper.setDeltaMovement(allowed.x != wanted.x ? 0 : velocity.x, allowed.y != wanted.y ? 0 : velocity.y,
                allowed.z != wanted.z ? 0 : velocity.z);
        detectImpact(wanted, moved);
    }

    private void detectImpact(Vec3 wanted, Vec3 moved) {
        double blockedX = isBlocked(wanted.x, moved.x) ? wanted.x : 0;
        double blockedZ = isBlocked(wanted.z, moved.z) ? wanted.z : 0;
        double horizontalSpeed = Math.sqrt(blockedX * blockedX + blockedZ * blockedZ);
        double fallSpeed = wanted.y < 0 && moved.y - wanted.y > BLOCKED_EPSILON ? -wanted.y : 0;
        if (JumperHull.impactFraction(horizontalSpeed, fallSpeed) <= 0) return;
        if (jumper.level().isClientSide) {
            ModNetwork.sendToServer(new JumperImpactPacket((float) horizontalSpeed, (float) fallSpeed));
        } else {
            jumper.hull().onImpact((float) horizontalSpeed, (float) fallSpeed);
        }
    }

    private static boolean isBlocked(double wanted, double moved) {
        return Math.abs(wanted - moved) > BLOCKED_EPSILON;
    }

    boolean probeGround() {
        for (double offset : PuddleJumperEntity.segmentOffsets()) {
            AABB segment = jumper.segmentBox(offset);
            AABB probe = new AABB(segment.minX + GROUND_PROBE_INSET, segment.minY - GROUND_PROBE_DEPTH, segment.minZ + GROUND_PROBE_INSET,
                    segment.maxX - GROUND_PROBE_INSET, segment.minY + GROUND_PROBE_OVERLAP, segment.maxZ - GROUND_PROBE_INSET);
            if (!jumper.level().noCollision(jumper, probe)) return true;
        }
        return false;
    }

    private static double smallerMagnitude(double a, double b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }
}
