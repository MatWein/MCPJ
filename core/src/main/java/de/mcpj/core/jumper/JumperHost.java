package de.mcpj.core.jumper;

import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Box;
import de.mcpj.core.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

/**
 * What the ship logic needs from the game: the ship's body in the world, world queries and effects.
 * Each platform module implements it (typically the ship entity itself).
 */
public interface JumperHost {
    // ------------------------------------------------------------------ body

    /** Bottom center of the ship. */
    Vec3d location();

    /** Position at the start of the current tick (for client-side animation). */
    Vec3d previousLocation();

    /** Heading in degrees (Minecraft yaw: 0 = south, 90 = west). */
    float yaw();

    float previousYaw();

    void setYaw(float yaw);

    Vec3d velocity();

    void setVelocity(Vec3d velocity);

    boolean onGround();

    boolean isClientSide();

    /** Whether this side moves the ship (the pilot's client, or the server when nobody steers). */
    boolean isControlledLocally();

    /** A player sits at the controls. */
    boolean hasPilot();

    /** Anyone is on board. */
    boolean hasPassengers();

    boolean isAlive();

    int tickCount();

    long gameTime();

    RandomGenerator random();

    JumperSync sync();

    // ------------------------------------------------------------------ world

    /** Movement still possible for {@code wanted} when the given hull segment collides with blocks. */
    Vec3d collide(Vec3d wanted, Box segment);

    /** Moves the main body (with collisions and step-up); returns the actual movement. */
    Vec3d moveBy(Vec3d movement);

    /** True if no block or solid entity overlaps the box. */
    boolean isFree(Box box);

    /** Height of water around the main body (0 if none). */
    double waterHeight();

    /** Fluid height (0..1) of lava in that block, or a negative value if there is no lava. */
    double lavaHeight(BlockPosition pos);

    /** Charging pad at that position: energy it hands over (at most {@code room}); 0 if there is none. */
    int chargeFromPad(BlockPosition pos, int room, int intervalTicks, float chargeFraction);

    /** Places a light block if the spot is free (air or water source); false if not possible. */
    boolean placeLight(BlockPosition pos, int level);

    /** Removes a light block placed earlier (restoring water if it was waterlogged). */
    void removeLight(BlockPosition pos);

    // ------------------------------------------------------------------ effects and actions

    void playSound(JumperSound sound, float volume, float pitch);

    /** Server: particles seen by all players around. */
    void serverParticles(JumperParticle particle, Vec3d at, int count, Vec3d spread, double speed);

    /** Client: a single particle with its own velocity. */
    void clientParticle(JumperParticle particle, Vec3d at, Vec3d velocity);

    /** The shield absorbed a hit: clients let it flash. */
    void showShieldHit();

    /** Vibration/game event for a hull hit ({@code attacker} as handed in with the damage, may be null). */
    void hullDamaged(@Nullable Object attacker);

    /** Mobs chasing a passenger give up (the ship just cloaked). */
    void loseMobPursuers(double range);

    /** Launches a drone from {@code spawn}; the platform resolves the pilot's target ({@code targetId}). */
    void launchDrone(Vec3d spawn, Vec3d velocity, int targetId);

    /** Pilot's client: reports a collision to the server, which applies the damage. */
    void reportImpact(float horizontalSpeed, float fallSpeed);

    // ------------------------------------------------------------------ destruction

    void ejectPassengers();

    /** Removes the ship from the world (without drops). */
    void discard();

    void explode(Vec3d at, float power);

    boolean entityDropsEnabled();

    void dropShipItem();

    void dropCargo();
}
