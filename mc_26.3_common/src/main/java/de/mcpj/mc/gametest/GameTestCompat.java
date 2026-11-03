package de.mcpj.mc.gametest;

import de.mcpj.mc.Mcpj;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.platform.McpjPlatform;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;

/** Minecraft API differences the shared game tests need; the same methods in every mc_&lt;version&gt;_common. */
final class GameTestCompat {
    private GameTestCompat() {}

    static Player mockPlayer(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    static Mob spawnZombie(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(EntityTypes.ZOMBIE, pos);
    }

    static Block modBlock(String path) {
        return BuiltInRegistries.BLOCK.getValue(Mcpj.id(path));
    }

    static BlockEntity blockEntity(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, BlockEntity.class);
    }

    /** Saves the jumper and loads the data into a new one (not added to the level). */
    @Nullable
    static PuddleJumperEntity saveAndLoad(GameTestHelper helper, PuddleJumperEntity jumper) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        jumper.saveWithoutId(output);
        PuddleJumperEntity copy = McpjPlatform.get().jumperType().create(helper.getLevel(), EntitySpawnReason.LOAD);
        if (copy != null) copy.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        return copy;
    }

    /** A new lightning bolt, not yet in the world. */
    static LightningBolt lightning(GameTestHelper helper) {
        return EntityTypes.LIGHTNING_BOLT.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
    }
}
