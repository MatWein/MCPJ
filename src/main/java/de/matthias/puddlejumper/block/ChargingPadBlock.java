package de.matthias.puddlejumper.block;

import de.matthias.puddlejumper.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Ladestation für den Puddle Jumper: nimmt Forge Energy von allen Seiten auf (Mekanism, Thermal, IE, ...).
 * Ein darauf gelandeter Jumper zieht Strom aus allen Stationen unter seinem Rumpf.
 * Zustände: aus (dunkel), versorgt (leuchtet, Landebahn-Markierung), lädt (hell).
 */
public class ChargingPadBlock extends BaseEntityBlock {
    public enum Mode implements StringRepresentable {
        OFF("off", 0), POWERED("powered", 10), CHARGING("charging", 15);

        private final String name;
        public final int light;

        Mode(String name, int light) {
            this.name = name;
            this.light = light;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ChargingPadBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.OFF).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, FACING);
    }

    /** Der Landepfeil zeigt in Blickrichtung des Platzierenden. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // BaseEntityBlock wäre sonst unsichtbar
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingPadBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModRegistry.CHARGING_PAD_BE.get(), ChargingPadBlockEntity::serverTick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator: Ladestand des Jumpers, der gerade hier lädt (0 ohne Jumper). */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ChargingPadBlockEntity pad ? pad.getComparatorLevel() : 0;
    }
}
