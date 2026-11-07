package de.mcpj.mc.gametest;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static de.mcpj.mc.gametest.GameTestSupport.spawnJumper;
import static de.mcpj.mc.gametest.GameTestSupport.vulnerablePlayer;

/** Seat game tests, shared by the loaders (each registers them with its own annotations). */
public final class JumperSeatTestCases {
    private JumperSeatTestCases() {}

    /** Every seat takes one player, each on its own spot inside the hull; the first one pilots, one more is refused. */
    public static void seatsFillUpToCapacity(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        List<Player> players = board(helper, jumper, JumperGeometry.SEAT_COUNT);
        seatAll(jumper);
        helper.assertTrue(jumper.getControllingPassenger() == players.get(0), "First passenger is not the pilot");
        for (int i = 0; i < players.size(); i++) {
            Vec3 seat = players.get(i).position();
            helper.assertTrue(jumper.fullBoundingBox().inflate(0, 1, 0).contains(seat), "Seat " + i + " is outside the hull");
            for (int j = 0; j < i; j++) {
                helper.assertTrue(players.get(j).position().distanceTo(seat) > 1.0, "Seats " + j + " and " + i + " overlap");
            }
        }
        Player extra = GameTestCompat.mockPlayer(helper);
        helper.assertTrue(!extra.startRiding(jumper), "A passenger boarded a full jumper");
        // forced boarding (e.g. /ride) ignores the seat count; the extra passenger shares the last seat
        helper.assertTrue(McCompat.ride(extra, jumper), "Forced boarding failed");
        jumper.positionRider(extra);
        helper.assertTrue(extra.position().distanceTo(players.get(players.size() - 1).position()) < 0.5, "Extra passenger has no seat");
        helper.succeed();
    }

    /** When the pilot leaves, the next passenger takes over the controls and moves to the pilot's seat. */
    public static void nextPassengerTakesOver(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        List<Player> players = board(helper, jumper, 3);
        seatAll(jumper);
        Vec3 pilotSeat = players.get(0).position();
        players.get(0).stopRiding();
        seatAll(jumper);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(jumper.getControllingPassenger() == players.get(1), "Second passenger did not take over");
            helper.assertTrue(players.get(1).position().distanceTo(pilotSeat) < 0.5, "New pilot is not on the pilot's seat");
            helper.assertTrue(players.get(2).getVehicle() == jumper, "Third passenger left the jumper");
            helper.succeed();
        });
    }

    /** Passengers who are not the pilot are protected by the hull and never travel through a portal on their own. */
    public static void passengersAreProtectedLikeThePilot(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        List<Player> players = board(helper, jumper, 2);
        Player passenger = players.get(1);
        helper.assertTrue(jumper.getControllingPassenger() != passenger, "Second passenger is the pilot");
        float health = passenger.getHealth();
        passenger.hurt(helper.getLevel().damageSources().generic(), 10f);
        helper.assertTrue(passenger.getHealth() == health, "Passenger was hurt: " + passenger.getHealth());
        helper.assertTrue(!GameTestHooks.get().mayTravelAlone(passenger, Level.NETHER), "Seated passenger may travel alone");
        helper.succeed();
    }

    /** Mock players are not ticked, so nothing moves them to their seats on its own. */
    private static void seatAll(PuddleJumperEntity jumper) {
        jumper.getPassengers().forEach(jumper::positionRider);
    }

    private static List<Player> board(GameTestHelper helper, PuddleJumperEntity jumper, int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Player player = vulnerablePlayer(helper);
            helper.assertTrue(McCompat.ride(player, jumper), "Passenger " + i + " could not board");
            players.add(player);
        }
        return players;
    }
}
