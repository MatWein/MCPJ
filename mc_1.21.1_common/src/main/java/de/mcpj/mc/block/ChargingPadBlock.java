package de.mcpj.mc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.core.pad.PadMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class ChargingPadBlock extends BaseEntityBlock {
    /** Block state for the core's {@link PadMode}. */
    public enum Mode implements StringRepresentable {
        OFF("off", PadMode.OFF), POWERED("powered", PadMode.POWERED), CHARGING("charging", PadMode.CHARGING);

        private final String serializedName;
        private final PadMode core;

        Mode(String serializedName, PadMode core) {
            this.serializedName = serializedName;
            this.core = core;
        }

        public PadMode core() {
            return core;
        }

        public static Mode of(PadMode mode) {
            return values()[mode.ordinal()];
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final MapCodec<ChargingPadBlock> CODEC = simpleCodec(ChargingPadBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public ChargingPadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.OFF).setValue(FACING, Direction.NORTH));
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(MODE).core().lightLevel();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return McpjPlatform.get().newChargingPad(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, McpjPlatform.get().chargingPadType(), ChargingPadBlockEntity::serverTick);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ChargingPadBlockEntity pad ? pad.getComparatorLevel() : 0;
    }
}
