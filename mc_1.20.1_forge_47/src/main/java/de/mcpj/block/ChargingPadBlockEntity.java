package de.mcpj.block;

import de.mcpj.core.pad.ChargingPad;
import de.mcpj.core.pad.ChargingPadHost;
import de.mcpj.core.pad.PadMode;
import de.mcpj.util.NbtSaveData;
import de.mcpj.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The charging pad in the world; its logic is the core's {@link ChargingPad}, Forge Energy goes in from all sides. */
public class ChargingPadBlockEntity extends BlockEntity implements ChargingPadHost {
    private final ChargingPad pad = new ChargingPad(this);
    private final IEnergyStorage storage = new PadEnergyStorage(pad);
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> storage);

    public ChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_PAD.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingPadBlockEntity entity) {
        entity.pad.serverTick(level.getGameTime());
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

    // ------------------------------------------------------------------ Forge

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) return energyCapability.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCapability = LazyOptional.of(() -> storage);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        pad.load(new NbtSaveData(tag));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        pad.save(new NbtSaveData(tag));
    }
}
