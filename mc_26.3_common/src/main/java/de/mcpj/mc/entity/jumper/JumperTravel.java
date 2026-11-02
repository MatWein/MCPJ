package de.mcpj.mc.entity.jumper;

import de.mcpj.core.math.Vec3d;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Teleports through portals and stargates (e.g. Just Stargate Mod) as one unit: the passengers stay seated and
 * the ship keeps its speed, turned along with its new heading.
 *
 * <p>Portal mods like JSG send every entity in range on its own. So seated passengers never travel alone (see
 * {@link #mustNotTravelAlone}); the jumper takes them along instead.
 */
public final class JumperTravel {
    private final PuddleJumperEntity jumper;
    @Nullable private Departure pendingTeleport;

    /** State before a teleport: who was on board, how fast and in which direction the ship flew. */
    record Departure(List<Entity> passengers, Vec3d velocity, float yaw) {}

    JumperTravel(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    /** Passengers and the hull segments only travel together with the jumper itself. */
    public static boolean mustNotTravelAlone(Entity entity) {
        return entity instanceof JumperSegment || entity.getVehicle() instanceof PuddleJumperEntity;
    }

    Departure depart() {
        return new Departure(List.copyOf(jumper.getPassengers()), jumper.core().serverVelocity(), jumper.getYRot());
    }

    /** Called on the new jumper after a dimension change: bring the passengers over and keep flying. */
    void arriveFromOtherDimension(Departure departure) {
        for (Entity passenger : departure.passengers()) {
            // Minecraft usually brings the passengers along itself; only seat those it lost on the way
            if (passenger instanceof ServerPlayer player && player.getVehicle() != jumper && jumper.level() instanceof ServerLevel level) {
                player.teleportTo(level, jumper.getX(), jumper.getY(), jumper.getZ(), Set.of(), jumper.getYRot(), player.getXRot(), false);
                player.startRiding(jumper, true, false);
            }
        }
        // the portal may also have positioned the new ship via teleportTo; this arrival is the one that counts
        pendingTeleport = null;
        continueFlight(departure);
    }

    /** The dimension change did not happen (e.g. cancelled after the passengers were thrown off): seat them again. */
    void travelFailed(Departure departure) {
        if (jumper.isRemoved()) return;
        for (Entity passenger : departure.passengers()) {
            if (passenger.getVehicle() == null && passenger.level() == jumper.level()) passenger.startRiding(jumper, true, false);
        }
    }

    /** Same-dimension teleport: the heading is often set right after the move, so finish on the next tick. */
    void teleportedWithinDimension(Departure departure) {
        // several moves in one tick (e.g. setPos + teleportTo): the state before the first one counts
        if (pendingTeleport == null) pendingTeleport = departure;
    }

    void tick() {
        if (pendingTeleport == null) return;
        continueFlight(pendingTeleport);
        pendingTeleport = null;
        // the pilot's client controls the ship and ignores server teleports of it: move it explicitly
        if (jumper.getControllingPassenger() instanceof ServerPlayer pilot) {
            pilot.connection.send(ClientboundMoveVehiclePacket.fromEntity(jumper));
        }
    }

    private void continueFlight(Departure departure) {
        jumper.core().continueAfterTeleport(departure.velocity(), departure.yaw());
    }
}
