package de.mcpj.mc.block;

import de.mcpj.core.pad.ChargingPad;
import de.mcpj.core.pad.ChargingPadHost;
import de.mcpj.core.pad.PadMode;
import de.mcpj.mc.util.NbtSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The charging pad in the world; its logic is the core's {@link ChargingPad}. Each loader subclass connects it to its
 * energy API (energy goes in from all sides).
 */
public class ChargingPadBlockEntity extends BlockEntity implements ChargingPadHost {
    private final ChargingPad pad = new ChargingPad(this);

    public ChargingPadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingPadBlockEntity entity) {
        entity.pad.serverTick(level.getGameTime());
    }

    /** The core logic, for the loader's energy API adapter. */
    protected ChargingPad pad() {
        return pad;
    }

    public int supplyJumper(int requested, int ticks, float jumperChargeFraction) {
        return pad.supplyJumper(requested, ticks, jumperChargeFraction);
    }

    public int getComparatorLevel() {
        return pad.comparatorLevel();
    }

    // ------------------------------------------------------------------ ChargingPadHost

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public void comparatorChanged() {
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    @Override
    public PadMode currentMode() {
        return getBlockState().getValue(ChargingPadBlock.MODE).core();
    }

    @Override
    public void setMode(PadMode mode) {
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(ChargingPadBlock.MODE, ChargingPadBlock.Mode.of(mode)), Block.UPDATE_ALL);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pad.load(NbtSaveData.reading(input));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        pad.save(NbtSaveData.writing(output));
    }
}
