package de.mcpj.mc.gametest;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.platform.McpjPlatform;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Minecraft API differences the shared game tests need; the same methods in every mc_&lt;version&gt;_common. */
final class GameTestCompat {
    private GameTestCompat() {}

    static Player mockPlayer(GameTestHelper helper) {
        return helper.makeMockPlayer();
    }

    static Mob spawnZombie(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(EntityType.ZOMBIE, pos);
    }

    static Block modBlock(String path) {
        return BuiltInRegistries.BLOCK.get(Mcpj.id(path));
    }

    static BlockEntity blockEntity(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    /** Saves the jumper and loads the data into a new one (not added to the level). */
    @Nullable
    static PuddleJumperEntity saveAndLoad(GameTestHelper helper, PuddleJumperEntity jumper) {
        CompoundTag tag = jumper.saveWithoutId(new CompoundTag());
        PuddleJumperEntity copy = McpjPlatform.get().jumperType().create(helper.getLevel());
        if (copy != null) copy.load(tag);
        return copy;
    }

    /** A new lightning bolt, not yet in the world. */
    static LightningBolt lightning(GameTestHelper helper) {
        return EntityType.LIGHTNING_BOLT.create(helper.getLevel());
    }

    /** Minecraft 1.20.1 has no way to name the exact target spot: each loader's GameTestHooks does it. */
    static Entity changeDimension(Entity entity, ServerLevel destination, Vec3 position, float yaw) {
        throw new UnsupportedOperationException("Minecraft 1.20.1: the loader changes dimensions (GameTestHooks)");
    }
}
