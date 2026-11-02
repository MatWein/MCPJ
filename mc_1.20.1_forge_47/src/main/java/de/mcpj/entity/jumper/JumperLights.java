package de.mcpj.entity.jumper;

import de.mcpj.core.config.LightConfig;
import de.mcpj.core.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft has no moving light sources, so the server places invisible vanilla light blocks
 * in the hull and ahead of the bow and removes them again behind the ship.
 */
public final class JumperLights {
    private static final int CENTER = 0;
    private static final int BOW = 1;
    private static final double STRANDED_DISTANCE_FACTOR = 2.0;
    private static final String LIGHT_BLOCKS_TAG = "LightBlocks";

    private final PuddleJumperEntity jumper;
    private final BlockPos[] placed = new BlockPos[2];
    private final List<BlockPos> leftovers = new ArrayList<>();

    JumperLights(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    public List<BlockPos> placedBlocks() {
        return Arrays.stream(placed).filter(Objects::nonNull).toList();
    }

    void tick() {
        leftovers.forEach(this::remove);
        leftovers.clear();
        if (!jumper.isLightsOn() || jumper.isCloaked() || !jumper.isAlive()) {
            clear();
            return;
        }
        LightConfig config = ServerConfig.LIGHTS;
        boolean fast = jumper.serverVelocity().length() > config.singleLightSpeedPerTick();
        boolean moveTick = jumper.tickCount % config.updateIntervalTicks() == 0;
        Vec3 center = jumper.position().add(0, JumperGeometry.CENTER_HEIGHT, 0);
        Vec3[] targets = {center, center.add(jumper.forward().scale(JumperGeometry.BOW_LIGHT_DISTANCE))};
        for (int slot = 0; slot < placed.length; slot++) {
            if (slot == CENTER && fast) release(slot);
            else update(slot, targets[slot], moveTick, config.moveDistance());
        }
    }

    private void update(int slot, Vec3 target, boolean moveTick, double moveDistance) {
        BlockPos current = placed[slot];
        if (current != null && (!moveTick || current.getCenter().distanceTo(target) <= moveDistance)) return;
        BlockPos next = BlockPos.containing(target);
        if (next.equals(current) || next.equals(placed[otherSlot(slot)])) return;
        if (place(next)) {
            if (current != null) remove(current);
            placed[slot] = next;
        } else if (current != null && current.getCenter().distanceTo(target) > moveDistance * STRANDED_DISTANCE_FACTOR) {
            release(slot);
        }
    }

    private static int otherSlot(int slot) {
        return slot == CENTER ? BOW : CENTER;
    }

    private void release(int slot) {
        if (placed[slot] != null) remove(placed[slot]);
        placed[slot] = null;
    }

    void clear() {
        for (int slot = 0; slot < placed.length; slot++) release(slot);
    }

    private boolean place(BlockPos pos) {
        Level level = jumper.level();
        if (!level.isLoaded(pos)) return false;
        BlockState existing = level.getBlockState(pos);
        boolean water = existing.is(Blocks.WATER) && existing.getFluidState().isSource();
        if (!existing.isAir() && !water) return false;
        BlockState light = Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, ServerConfig.LIGHTS.lightLevel())
                .setValue(BlockStateProperties.WATERLOGGED, water);
        return level.setBlock(pos, light, Block.UPDATE_CLIENTS);
    }

    private void remove(BlockPos pos) {
        Level level = jumper.level();
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.LIGHT)) return;
        BlockState restored = state.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, restored, Block.UPDATE_CLIENTS);
    }

    void load(CompoundTag tag) {
        leftovers.clear();
        for (long pos : tag.getLongArray(LIGHT_BLOCKS_TAG)) leftovers.add(BlockPos.of(pos));
    }

    void save(CompoundTag tag) {
        List<BlockPos> all = new ArrayList<>(leftovers);
        all.addAll(placedBlocks());
        tag.putLongArray(LIGHT_BLOCKS_TAG, all.stream().mapToLong(BlockPos::asLong).toArray());
    }
}
