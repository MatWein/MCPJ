package de.mcpj.block;

import de.mcpj.core.pad.ChargingPad;
import net.minecraftforge.energy.IEnergyStorage;

/** Forge Energy view of the core's charging pad: other mods can only insert energy. */
record PadEnergyStorage(ChargingPad pad) implements IEnergyStorage {
    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        return pad.receive(amount, simulate);
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return pad.stored();
    }

    @Override
    public int getMaxEnergyStored() {
        return pad.capacity();
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
