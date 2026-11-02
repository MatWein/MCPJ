package de.mcpj.mc.gametest;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.mc.util.Convert;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import static de.mcpj.mc.gametest.GameTestSupport.PLATFORM;
import static de.mcpj.mc.gametest.GameTestSupport.spawnJumper;

/** Light game tests, shared by the loaders (each registers them with its own annotations). */
public final class JumperLightTestCases {
    private JumperLightTestCases() {}


    public static void lightsPlaceAndRemoveBlocks(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        BlockPos center = BlockPos.containing(jumper.position().add(0, JumperGeometry.CENTER_HEIGHT, 0));
        jumper.setLightsOn(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(center).is(Blocks.LIGHT), "No light block while lights are on");
            jumper.setCloaked(true);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(helper.getLevel().getBlockState(center).isAir(), "Cloaked jumper should be dark");
                jumper.setCloaked(false);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(helper.getLevel().getBlockState(center).is(Blocks.LIGHT), "Light missing after decloaking");
                    jumper.discard();
                    helper.assertTrue(helper.getLevel().getBlockState(center).isAir(), "Light block left behind after removal");
                    helper.succeed();
                });
            });
        });
    }

    public static void lightsFollowLazily(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 8, 2, 12, 0);
        jumper.setLightsOn(true);
        double startX = jumper.getX();
        BlockPosition[] first = new BlockPosition[1];
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(jumper.lights().placedBlocks().size() == 2, "Expected 2 lights, got " + jumper.lights().placedBlocks().size());
            first[0] = jumper.lights().placedBlocks().get(0);
        });
        for (int step = 1; step <= 13; step++) {
            double x = startX + 0.3 * step;
            helper.runAfterDelay(2 + step, () -> jumper.setPos(x, jumper.getY(), jumper.getZ()));
        }
        helper.runAfterDelay(6, () -> helper.assertTrue(jumper.lights().placedBlocks().contains(first[0]), "Light moved after a small movement"));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!jumper.lights().placedBlocks().contains(first[0]), "Center light did not follow");
            helper.assertTrue(helper.getLevel().getBlockState(Convert.toMc(first[0])).isAir(), "Old light not removed");
            helper.assertTrue(jumper.lights().placedBlocks().size() == 2, "Expected 2 lights again");
            helper.succeed();
        });
    }
}
