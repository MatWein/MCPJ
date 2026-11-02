package de.matthias.puddlejumper.gametest;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.function.Function;

import static de.matthias.puddlejumper.gametest.GameTestSupport.PLATFORM;
import static de.matthias.puddlejumper.gametest.GameTestSupport.spawnJumper;

@GameTestHolder(PuddleJumperMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperTravelTests {
    private static final Vec3 NETHER_TARGET = new Vec3(0.5, 120, 0.5);

    @GameTest(template = PLATFORM)
    public static void jumperChangesDimensionWithItsState(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.cargo().setItem(3, new ItemStack(Items.DIAMOND, 5));
        jumper.hurt(helper.getLevel().damageSources().generic(), 24f);
        float hull = jumper.hull().integrity();
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        Entity arrived = jumper.changeDimension(nether, teleporterTo(NETHER_TARGET, 90f));
        helper.assertTrue(arrived instanceof PuddleJumperEntity, "Jumper did not arrive");
        PuddleJumperEntity moved = (PuddleJumperEntity) arrived;
        try {
            helper.assertTrue(moved.level() == nether, "Jumper is not in the nether");
            helper.assertTrue(jumper.isRemoved(), "Old jumper still exists");
            helper.assertTrue(moved.hull().integrity() == hull, "Hull not kept: " + moved.hull().integrity());
            helper.assertTrue(moved.cargo().getItem(3).is(Items.DIAMOND), "Cargo not kept");
        } finally {
            moved.discard();
        }
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void hullSegmentsNeverTravelAlone(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        for (PartEntity<?> part : jumper.getParts()) {
            helper.assertTrue(part.changeDimension(nether, teleporterTo(NETHER_TARGET, 0f)) == null, "A hull segment changed dimension");
            helper.assertTrue(!part.isRemoved(), "A hull segment was removed");
        }
        helper.assertTrue(!jumper.isRemoved(), "Jumper was removed");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void seatedPilotDoesNotTravelAlone(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player pilot = helper.makeMockPlayer();
        helper.assertTrue(pilot.startRiding(jumper, true), "Pilot could not board");
        helper.assertTrue(!ForgeHooks.onTravelToDimension(pilot, Level.NETHER), "Seated pilot may travel alone");
        pilot.stopRiding();
        helper.assertTrue(ForgeHooks.onTravelToDimension(pilot, Level.NETHER), "Player outside the jumper was blocked");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void teleportWithinDimensionKeepsPilot(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player pilot = helper.makeMockPlayer();
        helper.assertTrue(pilot.startRiding(jumper, true), "Pilot could not board");
        Vec3 target = jumper.position().add(0, 3, 0);
        jumper.teleportTo(target.x, target.y, target.z);
        helper.assertTrue(jumper.position().distanceTo(target) < 1.0e-6, "Jumper did not move");
        helper.assertTrue(pilot.getVehicle() == jumper, "Pilot left the jumper");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void destroyedSegmentDestroysShip(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        // e.g. a stargate kawoosh removes non-living entities directly
        jumper.getParts()[0].remove(Entity.RemovalReason.KILLED);
        helper.assertTrue(jumper.isRemoved(), "Ship survived losing a hull segment");
        helper.succeed();
    }

    private static ITeleporter teleporterTo(Vec3 pos, float yaw) {
        return new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(pos, Vec3.ZERO, yaw, 0f);
            }

            @Override
            public boolean isVanilla() {
                return false;
            }
        };
    }
}
