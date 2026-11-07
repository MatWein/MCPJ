package de.mcpj.mc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import de.mcpj.mc.platform.McpjPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/** Builds a jumper above itself after a redstone pulse (see JumperProjectorBlockEntity); its nose points in FACING. */
public class JumperProjectorBlock extends BaseEntityBlock {
    public static final BooleanProperty BUILDING = BooleanProperty.create("building");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final int BUILDING_LIGHT = 12;

    public static final MapCodec<JumperProjectorBlock> CODEC = simpleCodec(JumperProjectorBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public JumperProjectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BUILDING, false).setValue(FACING, Direction.NORTH));
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(BUILDING) ? BUILDING_LIGHT : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BUILDING, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JumperProjectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, McpjPlatform.get().projectorType(), JumperProjectorBlockEntity::serverTick);
    }
}
