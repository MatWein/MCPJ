package de.mcpj.gametest;

import de.mcpj.McpjMod;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.entity.drone.DroneEntity;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import de.mcpj.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static de.mcpj.gametest.GameTestSupport.PLATFORM;
import static de.mcpj.gametest.GameTestSupport.fillWater;
import static de.mcpj.gametest.GameTestSupport.spawnJumper;

@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperCombatTests {

    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void droneHitsZombie(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 4, 2, 12, -90f);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(20, 2, 6));
        zombie.setNoAi(true);
        float startHealth = zombie.getHealth();
        helper.runAfterDelay(2, () -> launch(helper, jumper, zombie, jumper.position().add(0, 1.2, 0), new Vec3(0.3, 0.3, 0.6)));
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getHealth() < startHealth || !zombie.isAlive(), "Zombie was not hit");
            helper.assertTrue(jumper.hull().integrity() == 1f, "Drone damaged its own jumper");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void droneHitsOtherJumper(GameTestHelper helper) {
        PuddleJumperEntity shooter = spawnJumper(helper, 4, 2, 4, 0);
        PuddleJumperEntity target = spawnJumper(helper, 18, 2, 18, 90f);
        helper.assertTrue(shooter.isValidTarget(target), "Other jumper should be targetable");
        helper.assertTrue(!shooter.isValidTarget(shooter), "Own jumper must not be a target");
        helper.runAfterDelay(2, () -> launch(helper, shooter, target, shooter.position().add(0, 2.5, 0), new Vec3(0, 0.3, 0.4)));
        helper.succeedWhen(() -> {
            helper.assertTrue(target.hull().integrity() < 1f || !target.isAlive(), "Target jumper was not hit");
            helper.assertTrue(shooter.hull().integrity() == 1f, "Shooter was damaged");
        });
    }

    private static void launch(GameTestHelper helper, PuddleJumperEntity jumper, net.minecraft.world.entity.Entity target, Vec3 from, Vec3 velocity) {
        DroneEntity drone = new DroneEntity(helper.getLevel(), helper.makeMockPlayer(), jumper, target);
        drone.setPos(from);
        drone.setDeltaMovement(velocity);
        helper.getLevel().addFreshEntity(drone);
    }

    @GameTest(template = PLATFORM)
    public static void ownDroneExplosionSparesJumper(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(12, 2, 17));
        zombie.setNoAi(true);
        float startHealth = zombie.getHealth();
        DroneEntity drone = new DroneEntity(helper.getLevel(), helper.makeMockPlayer(), jumper, null);
        Vec3 at = jumper.position().add(0, 1, 4.2);
        drone.setPos(at);
        helper.getLevel().addFreshEntity(drone);
        helper.getLevel().explode(drone, at.x, at.y, at.z, 2.0f, Level.ExplosionInteraction.NONE);
        helper.assertTrue(jumper.isAlive() && jumper.hull().integrity() == 1f, "Own drone explosion damaged the jumper");
        helper.assertTrue(zombie.getHealth() < startHealth, "Explosion did not hit the zombie");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void shieldMakesIndestructible(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.setShielded(true);
        DamageSource source = helper.getLevel().damageSources().generic();
        for (int i = 0; i < 50; i++) jumper.hurt(source, 20f);
        jumper.hurt(helper.getLevel().damageSources().playerAttack(helper.makeMockPlayer()), 1000f);
        helper.assertTrue(jumper.isAlive(), "Jumper destroyed despite shield");
        helper.assertTrue(jumper.hull().integrity() == 1f, "Shield let damage through");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void survivesOneDroneNotTwo(GameTestHelper helper) {
        PuddleJumperEntity target = spawnJumper(helper, 12, 2, 12, 0);
        PuddleJumperEntity attacker = spawnJumper(helper, 4, 2, 4, 0);
        var owner = helper.makeMockPlayer();
        DroneEntity first = new DroneEntity(helper.getLevel(), owner, attacker, target);
        target.hurt(helper.getLevel().damageSources().thrown(first, owner), 8f);
        target.hurt(helper.getLevel().damageSources().thrown(first, owner), 30f);
        float expected = 1f - ServerConfig.HULL.droneHitFraction();
        helper.assertTrue(target.isAlive(), "Jumper did not survive the first drone");
        helper.assertTrue(Math.abs(target.hull().integrity() - expected) < 0.01f, "Hull after one drone: " + target.hull().integrity());
        DroneEntity second = new DroneEntity(helper.getLevel(), owner, attacker, target);
        target.hurt(helper.getLevel().damageSources().thrown(second, owner), 8f);
        helper.assertTrue(target.isRemoved(), "Second drone did not destroy the jumper");
        helper.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void breaksAndDropsCargo(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.cargo().setItem(5, new ItemStack(Items.GOLD_INGOT, 9));
        DamageSource source = helper.getLevel().damageSources().generic();
        int hits = 0;
        while (jumper.isAlive() && hits < 200) {
            jumper.hurt(source, 7f);
            hits++;
        }
        helper.assertTrue(jumper.isRemoved(), "Jumper was not destroyed");
        helper.assertTrue(hits >= 20, "Jumper broke after only " + hits + " hits");
        helper.runAfterDelay(1, () -> {
            var items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, jumper.getBoundingBox().inflate(4));
            helper.assertTrue(items.stream().anyMatch(item -> item.getItem().is(ModItems.PUDDLE_JUMPER.get())), "Jumper item missing");
            helper.assertTrue(items.stream().anyMatch(item -> item.getItem().is(Items.GOLD_INGOT)), "Cargo was not dropped");
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void waterDamagesHull(GameTestHelper helper) {
        fillWater(helper);
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 3, 12, 0);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(jumper.isSubmerged(), "Jumper should be submerged");
            helper.assertTrue(jumper.hull().integrity() < 1f, "No water damage");
            helper.assertTrue(Math.abs(jumper.getY() - helper.absoluteVec(new Vec3(0, 3, 0)).y) < 0.01, "Jumper floated up to " + jumper.getY());
            helper.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void shieldBlocksWater(GameTestHelper helper) {
        fillWater(helper);
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 3, 12, 0);
        jumper.setShielded(true);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(jumper.isSubmerged(), "Jumper should be submerged");
            helper.assertTrue(jumper.hull().integrity() == 1f, "Damage despite shield");
            helper.succeed();
        });
    }
}
