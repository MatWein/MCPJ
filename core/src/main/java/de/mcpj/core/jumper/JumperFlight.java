package de.mcpj.core.jumper;

import de.mcpj.core.config.FlightConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.Box;
import de.mcpj.core.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Inert flight model. It runs where the ship is controlled (the pilot's client, otherwise the server):
 * thrust, turning, drag per medium, falling without energy, collisions of all three hull segments and impacts.
 */
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
    private static final int ARRIVAL_WINDOW_TICKS = 20;

    private final Jumper jumper;
    private float yawVelocity;
    private Vec3d ownVelocity = Vec3d.ZERO;
    @Nullable private Vec3d arrivalVelocity;
    private int arrivalTicks;

    JumperFlight(Jumper jumper) {
        this.jumper = jumper;
    }

    /** After a portal: whoever controls the ship (pilot client or server) continues with this velocity. */
    public void arrive(Vec3d velocity) {
        arrivalVelocity = velocity;
        arrivalTicks = ARRIVAL_WINDOW_TICKS;
    }

    void tick() {
        JumperHost host = jumper.host();
        takeOverArrivalVelocity();
        // External impulses (explosions, hits, players) are discarded: only our own velocity counts.
        host.setVelocity(ownVelocity);
        if (!host.isControlledLocally()) {
            host.setVelocity(Vec3d.ZERO);
            ownVelocity = Vec3d.ZERO;
            return;
        }
        if (host.hasPilot()) steer(FlightInput.source.get());
        else drift();
        moveWithSegments(host.velocity());
        ownVelocity = host.velocity();
    }

    private void takeOverArrivalVelocity() {
        if (arrivalVelocity == null) return;
        if (jumper.host().isControlledLocally()) {
            ownVelocity = arrivalVelocity;
            arrivalVelocity = null;
        } else if (--arrivalTicks <= 0) {
            arrivalVelocity = null;
        }
    }

    private void steer(FlightInput input) {
        JumperHost host = jumper.host();
        if (!jumper.energy().hasEnergy()) {
            fall();
            return;
        }
        FlightConfig config = ServerConfig.FLIGHT;
        JumperMedium medium = jumper.medium();
        double acceleration = config.accelerationPerTick() * config.accelerationFactor(medium);

        yawVelocity = yawVelocity * config.turnDrag() - input.turn() * config.turnAccelerationPerTick() * config.turnFactor(medium);
        if (input.turn() == 0 && Math.abs(yawVelocity) < TURN_REST_SPEED) yawVelocity = 0;
        host.setYaw(host.yaw() + yawVelocity);

        double forwardAcceleration = input.forward() > 0 ? acceleration : acceleration * config.reverseFactor();
        Vec3d thrust = jumper.forward().scale(input.forward() * forwardAcceleration)
                .add(jumper.left().scale(input.strafe() * acceleration * config.strafeFactor()))
                .add(0, input.vertical() * acceleration * config.verticalFactor(), 0);

        double drag = input.hasThrust() ? config.thrustDrag(medium) : config.idleDrag(medium);
        Vec3d velocity = host.velocity().scale(drag).add(thrust);
        double maxSpeed = config.maxSpeedPerTick();
        if (velocity.lengthSqr() > maxSpeed * maxSpeed) velocity = velocity.normalize().scale(maxSpeed);
        if (!input.hasThrust() && velocity.lengthSqr() < REST_SPEED_SQR) velocity = Vec3d.ZERO;
        host.setVelocity(velocity);
    }

    private void drift() {
        JumperHost host = jumper.host();
        if (!jumper.energy().hasEnergy() && !host.onGround()) {
            fall();
            return;
        }
        double drag = host.onGround() ? GROUND_FRICTION : ServerConfig.FLIGHT.idleDrag(jumper.medium());
        Vec3d velocity = host.velocity().scale(drag);
        host.setVelocity(velocity.lengthSqr() < REST_SPEED_SQR ? Vec3d.ZERO : velocity);
    }

    private void fall() {
        JumperHost host = jumper.host();
        Vec3d velocity = host.velocity();
        JumperMedium medium = jumper.medium();
        if (medium == JumperMedium.LAVA) {
            velocity = velocity.scale(LAVA_FALL_DRAG).add(0, -LAVA_GRAVITY, 0);
        } else if (medium == JumperMedium.WATER) {
            velocity = velocity.scale(WATER_FALL_DRAG).add(0, -WATER_GRAVITY, 0);
        } else {
            double horizontalDrag = host.onGround() ? GROUND_FRICTION : FALL_DRAG;
            velocity = new Vec3d(velocity.x() * horizontalDrag, velocity.y() * FALL_DRAG - GRAVITY, velocity.z() * horizontalDrag);
        }
        host.setVelocity(velocity.withY(Math.max(velocity.y(), -MAX_FALL_SPEED)));
    }

    private void moveWithSegments(Vec3d wanted) {
        JumperHost host = jumper.host();
        if (wanted.lengthSqr() < MIN_MOVE_SQR) {
            host.moveBy(Vec3d.ZERO);
            return;
        }
        // the main body collides on its own when moving; bow and stern are checked here
        Vec3d allowed = wanted;
        for (double offset : new double[]{JumperGeometry.SEGMENT_OFFSET, -JumperGeometry.SEGMENT_OFFSET}) {
            Vec3d collided = host.collide(allowed, jumper.segmentBox(offset));
            allowed = new Vec3d(smallerMagnitude(allowed.x(), collided.x()), smallerMagnitude(allowed.y(), collided.y()),
                    smallerMagnitude(allowed.z(), collided.z()));
        }
        Vec3d moved = host.moveBy(allowed);
        Vec3d velocity = host.velocity();
        host.setVelocity(new Vec3d(allowed.x() != wanted.x() ? 0 : velocity.x(), allowed.y() != wanted.y() ? 0 : velocity.y(),
                allowed.z() != wanted.z() ? 0 : velocity.z()));
        detectImpact(wanted, moved);
    }

    private void detectImpact(Vec3d wanted, Vec3d moved) {
        double blockedX = isBlocked(wanted.x(), moved.x()) ? wanted.x() : 0;
        double blockedZ = isBlocked(wanted.z(), moved.z()) ? wanted.z() : 0;
        double horizontalSpeed = Math.sqrt(blockedX * blockedX + blockedZ * blockedZ);
        double fallSpeed = wanted.y() < 0 && moved.y() - wanted.y() > BLOCKED_EPSILON ? -wanted.y() : 0;
        if (JumperHull.impactFraction(horizontalSpeed, fallSpeed) <= 0) return;
        if (jumper.host().isClientSide()) {
            jumper.host().reportImpact((float) horizontalSpeed, (float) fallSpeed);
        } else {
            jumper.hull().onImpact((float) horizontalSpeed, (float) fallSpeed);
        }
    }

    private static boolean isBlocked(double wanted, double moved) {
        return Math.abs(wanted - moved) > BLOCKED_EPSILON;
    }

    boolean probeGround() {
        for (double offset : Jumper.segmentOffsets()) {
            Box segment = jumper.segmentBox(offset);
            Box probe = new Box(segment.minX() + GROUND_PROBE_INSET, segment.minY() - GROUND_PROBE_DEPTH, segment.minZ() + GROUND_PROBE_INSET,
                    segment.maxX() - GROUND_PROBE_INSET, segment.minY() + GROUND_PROBE_OVERLAP, segment.maxZ() - GROUND_PROBE_INSET);
            if (!jumper.host().isFree(probe)) return true;
        }
        return false;
    }

    private static double smallerMagnitude(double a, double b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }
}
