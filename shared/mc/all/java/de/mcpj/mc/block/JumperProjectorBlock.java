package de.mcpj.mc.block;

import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.platform.McpjPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Builds a jumper above itself after a redstone pulse, or when a player right-clicks its top and confirms (see
 * JumperProjectorBlockEntity); its nose points in FACING.
 */
public class JumperProjectorBlock extends ModEntityBlock {
    public static final BooleanProperty BUILDING = BooleanProperty.create("building");
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Build progress shown on top: how many of the eight ring lights have turned yellow. */
    public static final int PROGRESS_STEPS = 8;
    public static final IntegerProperty PROGRESS = IntegerProperty.create("progress", 0, PROGRESS_STEPS);
    private static final int BUILDING_LIGHT = 12;
    /** Client: asks the player whether to start a build here (set by the client setup; does nothing on a server). */
    public static Consumer<BlockPos> startDialog = pos -> {};

    public JumperProjectorBlock(Properties properties) {
        super(properties, JumperProjectorBlock::new);
        registerDefaultState(stateDefinition.any().setValue(BUILDING, false).setValue(FACING, Direction.NORTH).setValue(PROGRESS, 0));
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(BUILDING) ? BUILDING_LIGHT : 0;
    }

    /** Right-click on top: the client asks for confirmation, then sends ProjectorStartPacket. */
    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP) return InteractionResult.PASS;
        if (state.getValue(BUILDING)) {
            if (!level.isClientSide()) McCompat.overlayMessage(player, Component.translatable("message.mcpj.projector_busy"));
        } else if (level.isClientSide()) {
            startDialog.accept(pos);
        }
        return McCompat.success(level);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BUILDING, FACING, PROGRESS);
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
