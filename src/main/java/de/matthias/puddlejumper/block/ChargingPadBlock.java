package de.matthias.puddlejumper.block;

import de.matthias.puddlejumper.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class ChargingPadBlock extends BaseEntityBlock {
    public enum Mode implements StringRepresentable {
        OFF("off", 0), POWERED("powered", 10), CHARGING("charging", 15);

        private final String serializedName;
        private final int lightLevel;

        Mode(String serializedName, int lightLevel) {
            this.serializedName = serializedName;
            this.lightLevel = lightLevel;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ChargingPadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.OFF).setValue(FACING, Direction.NORTH));
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(MODE).lightLevel;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingPadBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CHARGING_PAD.get(), ChargingPadBlockEntity::serverTick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ChargingPadBlockEntity pad ? pad.getComparatorLevel() : 0;
    }
}
