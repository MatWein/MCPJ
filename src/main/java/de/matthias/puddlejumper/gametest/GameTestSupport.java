package de.matthias.puddlejumper.gametest;

import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
import de.matthias.puddlejumper.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

final class GameTestSupport {
    static final String PLATFORM = "platform";

    private GameTestSupport() {}

    static PuddleJumperEntity spawnJumper(GameTestHelper helper, double x, double y, double z, float yaw) {
        PuddleJumperEntity jumper = helper.spawn(ModEntities.PUDDLE_JUMPER.get(), (float) x, (float) y, (float) z);
        jumper.setYRot(yaw);
        jumper.yRotO = yaw;
        return jumper;
    }

    static void fillWater(GameTestHelper helper) {
        for (int x = 8; x <= 16; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 6; z <= 18; z++) helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
            }
        }
    }
}
