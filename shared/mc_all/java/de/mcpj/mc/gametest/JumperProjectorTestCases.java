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
    private static final int PROJECTOR_X = 12;
    private static final int PROJECTOR_Z = 12;
    private static final int MAX_FLOOR_Y = 4;

    private JumperProjectorTestCases() {}

    /**
     * The block {@code height} above the platform floor (0 = right on it). The floor's height in test coordinates
     * differs between Minecraft versions (1.21.1 places the structure higher), so the tests look for it.
     */
    private static BlockPos onFloor(GameTestHelper helper, int x, int height, int z) {
        int floor = 0;
        while (floor < MAX_FLOOR_Y && !helper.getBlockState(new BlockPos(x, floor, z)).is(Blocks.SMOOTH_STONE)) floor++;
        return new BlockPos(x, floor + 1 + height, z);
    }

    private static BlockPos projector(GameTestHelper helper) {
        return onFloor(helper, PROJECTOR_X, 0, PROJECTOR_Z);
    }

    private static JumperProjectorBlockEntity placeProjector(GameTestHelper helper) {
        helper.setBlock(projector(helper), GameTestCompat.modBlock("jumper_projector"));
        return (JumperProjectorBlockEntity) GameTestCompat.blockEntity(helper, projector(helper));
    }

    private static List<PuddleJumperEntity> jumpers(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(projector(helper));
        return helper.getLevel().getEntitiesOfClass(PuddleJumperEntity.class, new AABB(center).inflate(8));
    }

    /** A redstone pulse builds exactly one jumper; it is frozen under construction until the build is complete. */
    public static void projectorBuildsJumperAfterPulse(GameTestHelper helper) {
        JumperProjectorBlockEntity projector = placeProjector(helper);
        helper.setBlock(onFloor(helper, PROJECTOR_X + 1, 0, PROJECTOR_Z), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            List<PuddleJumperEntity> built = jumpers(helper);
            helper.assertTrue(built.size() == 1, "Expected one jumper under construction, found " + built.size());
            PuddleJumperEntity jumper = built.get(0);
            helper.assertTrue(jumper.construction().isActive(), "Jumper is not under construction");
            helper.assertTrue(!McCompat.hurt(jumper, helper.getLevel().damageSources().generic(), 10f), "Jumper under construction took damage");
            helper.assertTrue(helper.getBlockState(projector(helper)).getValue(JumperProjectorBlock.BUILDING), "Projector does not show building");
            projector.logic().skipTo(0.5f);
            helper.runAfterDelay(2, () -> {
                int lights = helper.getBlockState(projector(helper)).getValue(JumperProjectorBlock.PROGRESS);
                helper.assertTrue(lights == JumperProjectorBlock.PROGRESS_STEPS / 2, "Projector shows " + lights + " progress lights at half build");
                projector.logic().skipTo(1f);
                helper.runAfterDelay(5, () -> {
                    helper.assertTrue(!jumper.construction().isActive() && jumper.isAlive(), "Jumper not finished");
                    helper.assertTrue(!projector.logic().isBuilding(), "Projector still building");
                    helper.assertTrue(!helper.getBlockState(projector(helper)).getValue(JumperProjectorBlock.BUILDING), "Projector still shows building");
                    helper.assertTrue(helper.getBlockState(projector(helper)).getValue(JumperProjectorBlock.PROGRESS) == 0, "Progress lights not reset");
                    // the redstone signal is still on: no new pulse, no second jumper
                    helper.runAfterDelay(10, () -> {
                        helper.assertTrue(jumpers(helper).size() == 1, "A second jumper was started without a new pulse");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** Without free space above the projector, a pulse does nothing. */
    public static void projectorNeedsSpace(GameTestHelper helper) {
        JumperProjectorBlockEntity projector = placeProjector(helper);
        helper.setBlock(onFloor(helper, 12, 2, 14), Blocks.STONE);
        helper.setBlock(onFloor(helper, PROJECTOR_X + 1, 0, PROJECTOR_Z), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(jumpers(helper).isEmpty(), "Jumper built into a block");
            helper.assertTrue(!projector.logic().isBuilding(), "Projector building without space");
            helper.succeed();
        });
    }

    /** Breaking the projector during the build removes the unfinished jumper. */
    public static void unfinishedJumperVanishesWithProjector(GameTestHelper helper) {
        placeProjector(helper);
        helper.setBlock(onFloor(helper, PROJECTOR_X + 1, 0, PROJECTOR_Z), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(jumpers(helper).size() == 1, "No jumper under construction");
            helper.setBlock(projector(helper), Blocks.AIR);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(jumpers(helper).isEmpty(), "Unfinished jumper stayed after the projector was removed");
                helper.succeed();
            });
        });
    }
}
