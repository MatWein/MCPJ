package de.mcpj.mc.gametest;

import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.navigation.JumperNavigation;
import de.mcpj.core.navigation.NavigationAction;
import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static de.mcpj.mc.gametest.GameTestSupport.spawnJumper;

/** Travel game tests, shared by the loaders (each registers them with its own annotations). */
public final class JumperTravelTestCases {
    private JumperTravelTestCases() {}

    private static final Vec3 NETHER_TARGET = new Vec3(0.5, 120, 0.5);

    public static void jumperChangesDimensionWithItsState(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.cargo().setItem(3, new ItemStack(Items.DIAMOND, 5));
        jumper.hurt(helper.getLevel().damageSources().generic(), 24f);
        float hull = jumper.hull().integrity();
        jumper.core().navigation().apply(NavigationAction.ADD, JumperNavigation.NONE, "Base", new BlockPosition(5, 64, 5), "minecraft:overworld");
        UUID jumperId = jumper.getUUID();
        jumper.core().lock().useKey(null, jumperId);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        Entity arrived = GameTestHooks.get().changeDimension(jumper, nether, NETHER_TARGET, 90f);
        helper.assertTrue(arrived instanceof PuddleJumperEntity, "Jumper did not arrive");
        PuddleJumperEntity moved = (PuddleJumperEntity) arrived;
        try {
            helper.assertTrue(moved.level() == nether, "Jumper is not in the nether");
            helper.assertTrue(jumper.isRemoved(), "Old jumper still exists");
            helper.assertTrue(moved.hull().integrity() == hull, "Hull not kept: " + moved.hull().integrity());
            helper.assertTrue(moved.cargo().getItem(3).is(Items.DIAMOND), "Cargo not kept");
            helper.assertTrue(moved.core().navigation().waypoints().size() == 1, "Navigation not kept");
            helper.assertTrue(moved.core().lock().locked(), "Lock not kept");
            helper.assertTrue(moved.getUUID().equals(jumperId), "Id not kept, the key would no longer fit");
        } finally {
            moved.discard();
        }
        helper.succeed();
    }

    public static void hullSegmentsNeverTravelAlone(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        for (Entity part : jumper.segments()) {
            helper.assertTrue(GameTestHooks.get().changeDimension(part, nether, NETHER_TARGET, 0f) == null, "A hull segment changed dimension");
            helper.assertTrue(!part.isRemoved(), "A hull segment was removed");
        }
        helper.assertTrue(!jumper.isRemoved(), "Jumper was removed");
        helper.succeed();
    }

    public static void seatedPilotDoesNotTravelAlone(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player pilot = GameTestCompat.mockPlayer(helper);
        helper.assertTrue(McCompat.ride(pilot, jumper), "Pilot could not board");
        helper.assertTrue(!GameTestHooks.get().mayTravelAlone(pilot, Level.NETHER), "Seated pilot may travel alone");
        pilot.stopRiding();
        helper.assertTrue(GameTestHooks.get().mayTravelAlone(pilot, Level.NETHER), "Player outside the jumper was blocked");
        helper.succeed();
    }

    public static void teleportWithinDimensionKeepsPilot(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player pilot = GameTestCompat.mockPlayer(helper);
        helper.assertTrue(McCompat.ride(pilot, jumper), "Pilot could not board");
        Vec3 target = jumper.position().add(0, 3, 0);
        jumper.teleportTo(target.x, target.y, target.z);
        helper.assertTrue(jumper.position().distanceTo(target) < 1.0e-6, "Jumper did not move");
        helper.assertTrue(pilot.getVehicle() == jumper, "Pilot left the jumper");
        helper.succeed();
    }

    public static void destroyedSegmentDestroysShip(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        // e.g. a stargate kawoosh removes non-living entities directly
        jumper.segments().get(0).remove(Entity.RemovalReason.KILLED);
        helper.assertTrue(jumper.isRemoved(), "Ship survived losing a hull segment");
        helper.succeed();
    }
}
