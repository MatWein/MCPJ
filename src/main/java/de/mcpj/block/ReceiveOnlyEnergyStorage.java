package de.mcpj.block;

import net.minecraftforge.energy.IEnergyStorage;

import java.util.function.IntSupplier;

public class ReceiveOnlyEnergyStorage implements IEnergyStorage {
    private final IntSupplier capacity;
    private final IntSupplier maxReceive;
    private final Runnable onChanged;
    private int stored;

    public ReceiveOnlyEnergyStorage(IntSupplier capacity, IntSupplier maxReceive, Runnable onChanged) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        int accepted = Math.min(Math.min(amount, maxReceive.getAsInt()), getMaxEnergyStored() - stored);
        if (accepted <= 0) return 0;
        if (!simulate) {
            stored += accepted;
            onChanged.run();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        return 0;
    }

    public int drain(int amount) {
        int drained = Math.min(amount, stored);
        if (drained > 0) {
            stored -= drained;
            onChanged.run();
        }
        return drained;
    }

    @Override
    public int getEnergyStored() {
        return stored;
    }

    public void setEnergyStored(int energy) {
        stored = Math.max(0, energy);
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity.getAsInt();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
