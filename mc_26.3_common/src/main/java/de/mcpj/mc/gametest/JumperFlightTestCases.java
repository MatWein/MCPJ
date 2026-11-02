package de.mcpj.mc.gametest;

import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.core.jumper.JumperHull;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static de.mcpj.mc.gametest.GameTestSupport.PLATFORM;
import static de.mcpj.mc.gametest.GameTestSupport.spawnJumper;

/** Flight game tests, shared by the loaders (each registers them with its own annotations). */
public final class JumperFlightTestCases {
    private JumperFlightTestCases() {}


    public static void hoversWithoutPilot(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 7, 12, 0);
        double startY = jumper.getY();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(!jumper.onGround(), "Jumper landed instead of hovering");
            helper.assertTrue(Math.abs(jumper.getY() - startY) < 0.01, "Jumper sank to " + jumper.getY());
            helper.assertTrue(jumper.isPodsOut(), "Pods should be out while airborne");
            helper.succeed();
        });
    }

    public static void solidEvenWhenCloaked(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.setCloaked(true);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            for (double offset : new double[]{-JumperGeometry.SEGMENT_OFFSET, 0, JumperGeometry.SEGMENT_OFFSET}) {
                AABB box = zombie.getBoundingBox().move(jumper.getX() - zombie.getX(), jumper.getY() - zombie.getY(),
                        jumper.getZ() + offset - zombie.getZ());
                helper.assertTrue(!helper.getLevel().noCollision(zombie, box), "No resistance at offset " + offset);
            }
            for (var part : jumper.segments()) {
                helper.assertTrue(!jumper.canCollideWith(part), "Jumper collides with its own segment");
            }
            helper.succeed();
        });
    }

    public static void segmentsFollowRotation(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 90f);
        helper.runAfterDelay(2, () -> {
            var parts = jumper.segments();
            helper.assertTrue(parts.size() == 2, "Expected 2 segments");
            helper.assertTrue(parts.get(0).getId() == jumper.getId() + 1 && parts.get(1).getId() == jumper.getId() + 2, "Segment ids out of sequence");
            double bowOffset = parts.get(0).getX() - jumper.getX();
            double sternOffset = parts.get(1).getX() - jumper.getX();
            helper.assertTrue(Math.abs(bowOffset + JumperGeometry.SEGMENT_OFFSET) < 0.01, "Bow segment misplaced: " + bowOffset);
            helper.assertTrue(Math.abs(sternOffset - JumperGeometry.SEGMENT_OFFSET) < 0.01, "Stern segment misplaced: " + sternOffset);
            helper.assertTrue(jumper.onGround(), "Jumper on the floor should count as landed (at " + helper.relativeVec(jumper.position()) + ", below: " + helper.getBlockState(net.minecraft.core.BlockPos.containing(helper.relativeVec(jumper.position())).below()) + ")");
            helper.assertTrue(!jumper.isPodsOut(), "Pods should be retracted when landed");
            helper.succeed();
        });
    }

    public static void ignoresKnockback(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 5, 12, 0);
        helper.runAfterDelay(2, () -> {
            Vec3 start = jumper.position();
            helper.getLevel().explode(null, jumper.getX() + 2, jumper.getY() + 1, jumper.getZ(), 3.0f, Level.ExplosionInteraction.NONE);
            jumper.push(2, 1, 2);
            jumper.setDeltaMovement(jumper.getDeltaMovement().add(3, 0, 3));
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(jumper.position().distanceTo(start) < 0.01, "Jumper was moved by " + jumper.position().subtract(start));
                helper.assertTrue(jumper.hull().integrity() < 1f, "Explosion should still damage the hull");
                helper.succeed();
            });
        });
    }

    public static void crashesWithoutEnergy(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 9, 12, 0);
        helper.runAfterDelay(1, () -> jumper.energy().set(0));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(jumper.onGround(), "Jumper without energy did not fall");
            float lost = 1 - jumper.hull().integrity();
            helper.assertTrue(lost > 0.02f, "No crash damage: " + lost);
            helper.assertTrue(lost <= ServerConfig.HULL.maxImpactFraction() + 0.001f, "More than the maximum impact damage: " + lost);
            helper.succeed();
        });
    }

    public static void impactDamageCurve(GameTestHelper helper) {
        float max = ServerConfig.HULL.maxImpactFraction();
        helper.assertTrue(JumperHull.impactFraction(0.3, 0) == 0, "Brushing an obstacle causes damage");
        helper.assertTrue(JumperHull.impactFraction(0, 0.7) == 0, "Landing at full sink rate causes damage");
        helper.assertTrue(JumperHull.impactFraction(0.7, 0) > 0.1f, "Medium impact too harmless");
        helper.assertTrue(JumperHull.impactFraction(1.0, 0) == max, "Full speed impact is not maximal");
        helper.assertTrue(JumperHull.impactFraction(5, 5) == max, "Impact damage exceeds the maximum");
        helper.succeed();
    }
}
