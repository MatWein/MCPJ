package de.mcpj.core.jumper;

import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Box;
import de.mcpj.core.math.MathUtil;
import de.mcpj.core.math.Vec3d;

/**
 * The Puddle Jumper's game logic, independent of Minecraft version and mod loader. The platform creates one per
 * ship entity, implements {@link JumperHost} and calls the tick methods in this order:
 * {@link #beforeTick()}, {@link #flightTick()} (after its own movement interpolation), {@link #probeGround()},
 * then {@link #clientTick()} or {@link #trackServerVelocity()} and {@link #serverTick()}.
 */
public final class Jumper {
    private static final double TELEPORT_DISTANCE_SQR = 9.0;

    private final JumperHost host;
    private final JumperFlight flight;
    private final JumperEnergy energy;
    private final JumperHull hull;
    private final JumperSystems systems;
    private final JumperPods pods;
    private final JumperWeapons weapons;
    private final JumperLights lights;
    private final JumperAnimation animation;

    private Vec3d lastServerPosition = Vec3d.ZERO;
    private Vec3d serverVelocity = Vec3d.ZERO;
    private int lavaCheckTick = -1;
    private boolean touchesLava;

    public Jumper(JumperHost host) {
        this.host = host;
        this.flight = new JumperFlight(this);
        this.energy = new JumperEnergy(this);
        this.hull = new JumperHull(this);
        this.systems = new JumperSystems(this);
        this.pods = new JumperPods(this);
        this.weapons = new JumperWeapons(this);
        this.lights = new JumperLights(this);
        this.animation = new JumperAnimation(this);
    }

    public JumperHost host() {
        return host;
    }

    public JumperSync sync() {
        return host.sync();
    }

    public JumperFlight flight() {
        return flight;
    }

    public JumperEnergy energy() {
        return energy;
    }

    public JumperHull hull() {
        return hull;
    }

    public JumperSystems systems() {
        return systems;
    }

    public JumperPods pods() {
        return pods;
    }

    public JumperWeapons weapons() {
        return weapons;
    }

    public JumperLights lights() {
        return lights;
    }

    public JumperAnimation animation() {
        return animation;
    }

    // ------------------------------------------------------------------ ticking

    public void beforeTick() {
        if (!host.isClientSide()) energy.initialize();
        hull.tickHurtAnimation();
    }

    public void flightTick() {
        flight.tick();
    }

    public boolean probeGround() {
        return flight.probeGround();
    }

    public void clientTick() {
        animation.tick();
    }

    /** Server: the ship's speed from its position change (the pilot's client moves it). */
    public void trackServerVelocity() {
        Vec3d position = host.location();
        serverVelocity = position.subtract(lastServerPosition);
        if (serverVelocity.lengthSqr() > TELEPORT_DISTANCE_SQR) serverVelocity = Vec3d.ZERO;
        lastServerPosition = position;
    }

    public void serverTick() {
        pods.tick();
        hull.tick();
        energy.tick();
        lights.tick();
    }

    /** The ship was added to the world. */
    public void onAdded() {
        if (!host.isClientSide()) energy.initialize();
    }

    /**
     * After a portal or teleport: keep flying with the speed from before, turned along with the new heading,
     * and announce it to clients (the pilot's client controls the ship).
     */
    public void continueAfterTeleport(Vec3d velocityBefore, float yawBefore) {
        Vec3d velocity = velocityBefore.yRot((yawBefore - host.yaw()) * MathUtil.DEG_TO_RAD);
        flight.arrive(velocity);
        sync().announceArrival(velocity);
    }

    /** Client: the server announced an arrival. */
    public void onArrivalSynced() {
        flight.arrive(sync().arrivalVelocity());
    }

    /** Server: the ship is removed from the world. */
    public void onRemoved() {
        lights.clear();
    }

    // ------------------------------------------------------------------ state

    public boolean isCloaked() {
        return sync().cloaked();
    }

    public boolean isShielded() {
        return sync().shielded();
    }

    public boolean isLightsOn() {
        return sync().lightsOn();
    }

    public boolean isPodsOut() {
        return sync().podsOut();
    }

    public Vec3d serverVelocity() {
        return serverVelocity;
    }

    public boolean enginesRunning() {
        return energy.hasEnergy() && !host.onGround();
    }

    public boolean isSubmerged() {
        return host.waterHeight() > JumperGeometry.CENTER_HEIGHT;
    }

    /** True if any lava (source or flowing, e.g. poured from a bucket) reaches into the hull. */
    public boolean touchesLava() {
        if (lavaCheckTick != host.tickCount()) {
            lavaCheckTick = host.tickCount();
            touchesLava = findLava();
        }
        return touchesLava;
    }

    public JumperMedium medium() {
        if (touchesLava()) return JumperMedium.LAVA;
        return isSubmerged() ? JumperMedium.WATER : JumperMedium.AIR;
    }

    private boolean findLava() {
        for (double offset : segmentOffsets()) {
            Box box = segmentBox(offset);
            for (int x = floor(box.minX()); x <= floor(box.maxX()); x++) {
                for (int y = floor(box.minY()); y <= floor(box.maxY()); y++) {
                    for (int z = floor(box.minZ()); z <= floor(box.maxZ()); z++) {
                        double height = host.lavaHeight(new BlockPosition(x, y, z));
                        if (height >= 0 && y + height > box.minY()) return true;
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ geometry

    public Vec3d forward() {
        return MathUtil.forward(host.yaw());
    }

    public Vec3d left() {
        return MathUtil.left(host.yaw());
    }

    /** Model coordinates (x = left, y = up, z = forward, in model units) to world coordinates. */
    public Vec3d modelToWorld(double modelX, double modelY, double modelZ) {
        double scale = JumperGeometry.MODEL_SCALE;
        return host.location().add(left().scale(modelX * scale)).add(forward().scale(modelZ * scale)).add(0, modelY * scale, 0);
    }

    /** Where the passenger on the given seat sits (0 is the pilot, see {@link JumperGeometry#SEAT_COUNT}). */
    public Vec3d seatPosition(int seat) {
        return modelToWorld(JumperGeometry.seatX(seat), 0, JumperGeometry.seatZ(seat));
    }

    /** One of the three hull segments (see {@link #segmentOffsets()}) as a box in the world. */
    public Box segmentBox(double offset) {
        return Box.around(host.location().add(forward().scale(offset)), JumperGeometry.WIDTH / 2.0, JumperGeometry.HEIGHT);
    }

    /** Offsets of the bow, center and stern segment along the heading. */
    public static double[] segmentOffsets() {
        return new double[]{JumperGeometry.SEGMENT_OFFSET, 0, -JumperGeometry.SEGMENT_OFFSET};
    }

    public Box fullBox() {
        return segmentBox(0).union(segmentBox(JumperGeometry.SEGMENT_OFFSET)).union(segmentBox(-JumperGeometry.SEGMENT_OFFSET));
    }

    static int floor(double value) {
        return (int) Math.floor(value);
    }
}
