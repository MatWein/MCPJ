package de.mcpj.mc.gametest;

import de.mcpj.mc.block.JumperProjectorBlock;
import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.block.JumperProjectorBlockEntity;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Jumper projector game tests, shared by the loaders (each registers them with its own annotations). */
public final class JumperProjectorTestCases {
    // the projector faces north (default): the jumper stands on it along the z axis, x 11.07..13.93, z 8.1..16.9
    private static final BlockPos PROJECTOR = new BlockPos(12, 1, 12);
    private static final BlockPos SWITCH = new BlockPos(13, 1, 12);

    private JumperProjectorTestCases() {}

    private static JumperProjectorBlockEntity placeProjector(GameTestHelper helper) {
        helper.setBlock(PROJECTOR, GameTestCompat.modBlock("jumper_projector"));
        return (JumperProjectorBlockEntity) GameTestCompat.blockEntity(helper, PROJECTOR);
    }

    private static List<PuddleJumperEntity> jumpers(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(PROJECTOR);
        return helper.getLevel().getEntitiesOfClass(PuddleJumperEntity.class, new AABB(center).inflate(8));
    }

    /** A redstone pulse builds exactly one jumper; it is frozen under construction until the build is complete. */
    public static void projectorBuildsJumperAfterPulse(GameTestHelper helper) {
        JumperProjectorBlockEntity projector = placeProjector(helper);
        helper.setBlock(SWITCH, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            List<PuddleJumperEntity> built = jumpers(helper);
            helper.assertTrue(built.size() == 1, "Expected one jumper under construction, found " + built.size());
            PuddleJumperEntity jumper = built.get(0);
            helper.assertTrue(jumper.construction().isActive(), "Jumper is not under construction");
            helper.assertTrue(!McCompat.hurt(jumper, helper.getLevel().damageSources().generic(), 10f), "Jumper under construction took damage");
            helper.assertTrue(helper.getBlockState(PROJECTOR).getValue(JumperProjectorBlock.BUILDING), "Projector does not show building");
            projector.logic().skipTo(1f);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(!jumper.construction().isActive() && jumper.isAlive(), "Jumper not finished");
                helper.assertTrue(!projector.logic().isBuilding(), "Projector still building");
                helper.assertTrue(!helper.getBlockState(PROJECTOR).getValue(JumperProjectorBlock.BUILDING), "Projector still shows building");
                // the redstone signal is still on: no new pulse, no second jumper
                helper.runAfterDelay(10, () -> {
                    helper.assertTrue(jumpers(helper).size() == 1, "A second jumper was started without a new pulse");
                    helper.succeed();
                });
            });
        });
    }

    /** Without free space above the projector, a pulse does nothing. */
    public static void projectorNeedsSpace(GameTestHelper helper) {
        JumperProjectorBlockEntity projector = placeProjector(helper);
        helper.setBlock(new BlockPos(12, 3, 14), Blocks.STONE);
        helper.setBlock(SWITCH, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(jumpers(helper).isEmpty(), "Jumper built into a block");
            helper.assertTrue(!projector.logic().isBuilding(), "Projector building without space");
            helper.succeed();
        });
    }

    /** Breaking the projector during the build removes the unfinished jumper. */
    public static void unfinishedJumperVanishesWithProjector(GameTestHelper helper) {
        placeProjector(helper);
        helper.setBlock(SWITCH, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(jumpers(helper).size() == 1, "No jumper under construction");
            helper.setBlock(PROJECTOR, Blocks.AIR);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(jumpers(helper).isEmpty(), "Unfinished jumper stayed after the projector was removed");
                helper.succeed();
            });
        });
    }
}
