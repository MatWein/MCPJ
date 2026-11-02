package de.matthias.puddlejumper.entity.jumper;

import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

import static de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity.ARRIVALS;
import static de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity.ARRIVAL_VELOCITY;

/**
 * Teleports through portals and stargates (e.g. Just Stargate Mod) as one unit: the passengers stay seated and
 * the ship keeps its speed, turned along with its new heading.
 *
 * <p>Vanilla {@link Entity#changeDimension} throws all passengers off before the vehicle travels, and portal mods
 * like JSG send every entity in range on its own. So seated passengers never travel alone (see
 * {@link #mustNotTravelAlone}); the jumper takes them along instead.
 */
public final class JumperTravel {
    private final PuddleJumperEntity jumper;
    @Nullable private Departure pendingTeleport;

    /** State before a teleport: who was on board, how fast and in which direction the ship flew. */
    record Departure(List<Entity> passengers, Vec3 velocity, float yaw) {}

    JumperTravel(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    /** Passengers and the hull segments only travel together with the jumper itself. */
    public static boolean mustNotTravelAlone(Entity entity) {
        return entity instanceof PuddleJumperPart || entity.getVehicle() instanceof PuddleJumperEntity;
    }

    Departure depart() {
        return new Departure(List.copyOf(jumper.getPassengers()), jumper.serverVelocity(), jumper.getYRot());
    }

    /** Called on the new jumper after a dimension change: bring the passengers over and keep flying. */
    void arriveFromOtherDimension(Departure departure) {
        for (Entity passenger : departure.passengers()) {
            if (passenger instanceof ServerPlayer player && jumper.level() instanceof ServerLevel level) {
                player.teleportTo(level, jumper.getX(), jumper.getY(), jumper.getZ(), jumper.getYRot(), player.getXRot());
                player.startRiding(jumper, true);
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
            if (passenger.getVehicle() == null && passenger.level() == jumper.level()) passenger.startRiding(jumper, true);
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
            pilot.connection.send(new ClientboundMoveVehiclePacket(jumper));
        }
    }

    private void continueFlight(Departure departure) {
        Vec3 velocity = departure.velocity().yRot((departure.yaw() - jumper.getYRot()) * Mth.DEG_TO_RAD);
        jumper.flight().arrive(velocity);
        jumper.getEntityData().set(ARRIVAL_VELOCITY, velocity.toVector3f());
        jumper.getEntityData().set(ARRIVALS, jumper.getEntityData().get(ARRIVALS) + 1);
    }

    /** Client: the server announced an arrival; the pilot's client takes over the speed. */
    void onArrivalSynced() {
        Vector3f velocity = jumper.getEntityData().get(ARRIVAL_VELOCITY);
        jumper.flight().arrive(new Vec3(velocity));
    }
}
