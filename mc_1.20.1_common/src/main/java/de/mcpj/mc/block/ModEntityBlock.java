package de.mcpj.mc.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Function;

/**
 * Minecraft 1.20.1's side of MCPJ's blocks with a block entity: what differs between Minecraft versions (codec,
 * render shape, comparator output). The blocks themselves are version-independent.
 */
public abstract class ModEntityBlock extends BaseEntityBlock {
    protected ModEntityBlock(Properties properties, Function<Properties, ? extends ModEntityBlock> factory) {
        super(properties);
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return hasComparatorOutput();
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return comparatorOutput(level, pos);
    }
}
