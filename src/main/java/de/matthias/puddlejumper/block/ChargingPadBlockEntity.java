package de.matthias.puddlejumper.block;

import de.matthias.puddlejumper.Config;
import de.matthias.puddlejumper.ModRegistry;
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

/** Energiepuffer der Ladestation (Forge Energy, nur Aufnahme - Abgabe ausschließlich an den Jumper). */
public class ChargingPadBlockEntity extends BlockEntity {
    /** So lange (Ticks) gilt die Station nach dem letzten Ladevorgang als "lädt". */
    private static final int CHARGING_HOLD = 10;

    private int energy;
    private int chargingTimer;
    private int comparatorLevel;

    private final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.min(Math.min(maxReceive, Config.PAD_INPUT_RATE.get()), getMaxEnergyStored() - energy);
            if (accepted <= 0) return 0;
            if (!simulate) {
                energy += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return Config.PAD_BUFFER.get();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> storage);

    public ChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.CHARGING_PAD_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingPadBlockEntity pad) {
        if (pad.chargingTimer > 0 && --pad.chargingTimer == 0) pad.setComparatorLevel(0);
        if (level.getGameTime() % 5 != 0) return;
        ChargingPadBlock.Mode mode = pad.chargingTimer > 0 ? ChargingPadBlock.Mode.CHARGING
                : pad.energy > 0 ? ChargingPadBlock.Mode.POWERED : ChargingPadBlock.Mode.OFF;
        if (state.getValue(ChargingPadBlock.MODE) != mode) {
            level.setBlock(pos, state.setValue(ChargingPadBlock.MODE, mode), Block.UPDATE_ALL);
        }
    }

    /**
     * Vom Jumper aufgerufen: bis zu {@code max} FE abgeben (höchstens die Ladeleistung × Ticks).
     * @param jumperFill Ladestand des Jumpers 0..1 (für den Comparator)
     */
    public int drawForJumper(int max, int ticks, float jumperFill) {
        int amount = Math.min(max, Math.min(energy, Config.PAD_CHARGE_RATE.get() * ticks));
        chargingTimer = CHARGING_HOLD;
        setComparatorLevel(Math.round(jumperFill * 15f));
        if (amount > 0) {
            energy -= amount;
            setChanged();
        }
        return amount;
    }

    public int getEnergy() {
        return energy;
    }

    public int getComparatorLevel() {
        return comparatorLevel;
    }

    private void setComparatorLevel(int v) {
        if (v == comparatorLevel) return;
        comparatorLevel = v;
        if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> storage);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getInt("Energy");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy);
    }
}
