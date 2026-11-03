package de.mcpj.forge.block;

import de.mcpj.core.energy.EnergyInput;
import net.minecraftforge.energy.IEnergyStorage;

/** Forge Energy view of a block that takes energy (charging pad, projector): other mods can only insert energy. */
public record InputEnergyStorage(EnergyInput input) implements IEnergyStorage {
    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        return input.receive(amount, simulate);
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return input.stored();
    }

    @Override
    public int getMaxEnergyStored() {
        return input.capacity();
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
