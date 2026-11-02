package de.matthias.puddlejumper.gametest;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.config.ServerConfig;
import de.matthias.puddlejumper.entity.jumper.JumperMedium;
import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static de.matthias.puddlejumper.gametest.GameTestSupport.PLATFORM;
import static de.matthias.puddlejumper.gametest.GameTestSupport.spawnJumper;

@GameTestHolder(PuddleJumperMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperHazardTests {

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void lavaDamagesHull(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        helper.setBlock(new BlockPos(12, 2, 12), Blocks.LAVA);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(jumper.medium() == JumperMedium.LAVA, "Lava not detected");
            float perSecond = ServerConfig.HULL.lavaDamagePerSecond() / ServerConfig.HULL.maxHull();
            float lost = 1f - jumper.hull().integrity();
            helper.assertTrue(lost >= 2 * perSecond - 1e-4f, "Too little lava damage: " + lost);
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void flowingLavaFromAboveDamagesHull(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        // the source sits above the hull: only the lava flowing down touches it, like a poured bucket
        BlockPos source = new BlockPos(12, 5, 12);
        helper.assertTrue(!jumper.fullBoundingBox().intersects(new AABB(helper.absolutePos(source))),
                "Lava source must be outside the hull");
        helper.setBlock(source, Blocks.LAVA);
        helper.succeedWhen(() -> {
            helper.assertTrue(jumper.touchesLava(), "Flowing lava not detected yet");
            helper.assertTrue(jumper.hull().integrity() < 1f, "No damage from flowing lava yet");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void shieldAbsorbsLava(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.setShielded(true);
        helper.setBlock(new BlockPos(12, 2, 12), Blocks.LAVA);
        helper.runAfterDelay(1, () -> {
            int before = jumper.energy().stored();
            helper.runAfterDelay(50, () -> {
                helper.assertTrue(jumper.hull().integrity() == 1f, "Hull damaged despite shield: " + jumper.hull().integrity());
                helper.assertTrue(jumper.energy().stored() < before, "Shield did not use energy");
                helper.succeed();
            });
        });
    }

    @GameTest(template = PLATFORM)
    public static void passengerIsInvulnerable(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player pilot = vulnerablePlayer(helper);
        helper.assertTrue(pilot.startRiding(jumper, true), "Pilot could not board");
        float health = pilot.getHealth();
        DamageSource generic = helper.getLevel().damageSources().generic();
        pilot.hurt(generic, 10f);
        pilot.hurt(helper.getLevel().damageSources().lava(), 10f);
        helper.assertTrue(pilot.getHealth() == health, "Passenger was hurt: " + pilot.getHealth());
        pilot.stopRiding();
        pilot.hurt(generic, 4f);
        helper.assertTrue(pilot.getHealth() < health, "Player outside the jumper should take damage");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void destructionEjectsPilotAndExplodes(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player pilot = vulnerablePlayer(helper);
        helper.assertTrue(pilot.startRiding(jumper, true), "Pilot could not board");
        Zombie bystander = helper.spawn(EntityType.ZOMBIE, new BlockPos(16, 2, 12));
        bystander.setNoAi(true);
        float bystanderHealth = bystander.getHealth();
        DamageSource source = helper.getLevel().damageSources().generic();
        for (int hits = 0; jumper.isAlive() && hits < 200; hits++) jumper.hurt(source, 7f);
        helper.assertTrue(jumper.isRemoved(), "Jumper was not destroyed");
        helper.assertTrue(pilot.getVehicle() == null, "Pilot was not thrown out");
        helper.assertTrue(!bystander.isAlive() || bystander.getHealth() < bystanderHealth, "No explosion damage");
        helper.succeed();
    }

    /** The mock player is creative and invulnerable by default; make it a normal survival player. */
    private static Player vulnerablePlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        player.getAbilities().invulnerable = false;
        player.getAbilities().instabuild = false;
        player.setPos(helper.absoluteVec(new Vec3(12, 2, 12)));
        return player;
    }
}
