package de.mcpj.core.drone;

import de.mcpj.core.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.random.RandomGenerator;

/** What the drone logic needs from the game (typically the drone entity itself). */
public interface DroneHost {
    Vec3d location();

    Vec3d velocity();

    boolean inWater();

    RandomGenerator random();

    /** Center of the current target if it still lives, otherwise null. */
    @Nullable
    Vec3d liveTargetCenter();

    /**
     * Picks the nearest hostile within {@code range} that lies ahead ({@code direction} · towards it greater than
     * {@code minAlignment}; any direction if {@code direction} is zero) and that the drone may hit.
     */
    void acquireTarget(Vec3d direction, double range, double minAlignment);

    /** Client: one particle. */
    void clientParticle(DroneParticle particle, Vec3d at, Vec3d velocity);

    /** Server: particles seen by all players around. */
    void serverParticles(DroneParticle particle, Vec3d at, int count, double spread, double speed);

    void playFizzleSound();
}
