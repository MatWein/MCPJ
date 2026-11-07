package de.mcpj.mc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Function;

/**
 * Minecraft 1.21.1's side of MCPJ's blocks with a block entity: what differs between Minecraft versions (codec,
 * render shape, comparator output). The blocks themselves are version-independent.
 */
public abstract class ModEntityBlock extends BaseEntityBlock {
    private final MapCodec<? extends BaseEntityBlock> codec;

    protected ModEntityBlock(Properties properties, Function<Properties, ? extends ModEntityBlock> factory) {
        super(properties);
        codec = simpleCodec(factory::apply);
    }

    /** Whether the block gives a comparator signal (see {@link #comparatorOutput}). */
    protected boolean hasComparatorOutput() {
        return false;
    }

    /** The comparator signal 0..15. */
    protected int comparatorOutput(Level level, BlockPos pos) {
        return 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return hasComparatorOutput();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return comparatorOutput(level, pos);
    }

    /** Right-click on the block (not sneaking); PASS lets the held item do its own thing. */
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onUse(state, level, pos, player, hit);
    }
}
