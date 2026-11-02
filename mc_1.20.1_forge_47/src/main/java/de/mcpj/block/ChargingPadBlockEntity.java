package de.mcpj.block;

import de.mcpj.config.ServerConfig;
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

public class ChargingPadBlockEntity extends BlockEntity {
    private static final int CHARGING_HOLD_TICKS = 10;
    private static final int STATE_UPDATE_INTERVAL = 5;
    private static final int MAX_COMPARATOR_LEVEL = 15;
    private static final String ENERGY_TAG = "Energy";

    private final ReceiveOnlyEnergyStorage storage = new ReceiveOnlyEnergyStorage(
            () -> ServerConfig.CHARGING_PAD.buffer(), () -> ServerConfig.CHARGING_PAD.inputRate(), this::setChanged);
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> storage);
    private int chargingTimer;
    private int comparatorLevel;

    public ChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_PAD.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingPadBlockEntity pad) {
        if (pad.chargingTimer > 0 && --pad.chargingTimer == 0) pad.setComparatorLevel(0);
        if (level.getGameTime() % STATE_UPDATE_INTERVAL != 0) return;
        ChargingPadBlock.Mode mode = pad.currentMode();
        if (state.getValue(ChargingPadBlock.MODE) != mode) {
            level.setBlock(pos, state.setValue(ChargingPadBlock.MODE, mode), Block.UPDATE_ALL);
        }
    }

    private ChargingPadBlock.Mode currentMode() {
        if (chargingTimer > 0) return ChargingPadBlock.Mode.CHARGING;
        return storage.getEnergyStored() > 0 ? ChargingPadBlock.Mode.POWERED : ChargingPadBlock.Mode.OFF;
    }

    public int supplyJumper(int requested, int ticks, float jumperChargeFraction) {
        chargingTimer = CHARGING_HOLD_TICKS;
        setComparatorLevel(Math.round(jumperChargeFraction * MAX_COMPARATOR_LEVEL));
        return storage.drain(Math.min(requested, ServerConfig.CHARGING_PAD.chargeRate() * ticks));
    }

    public int getComparatorLevel() {
        return comparatorLevel;
    }

    private void setComparatorLevel(int level) {
        if (level == comparatorLevel) return;
        comparatorLevel = level;
        if (this.level != null) this.level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

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
        storage.setEnergyStored(tag.getInt(ENERGY_TAG));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(ENERGY_TAG, storage.getEnergyStored());
    }
}
