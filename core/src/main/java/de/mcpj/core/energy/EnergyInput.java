package de.mcpj.core.energy;

/**
 * A block that takes energy from cables and machines (insert only), for the loaders' energy API adapters
 * (Forge Energy, NeoForge, Team Reborn Energy).
 */
public interface EnergyInput {
    /** Energy from cables or machines; returns the amount accepted. */
    int receive(int amount, boolean simulate);

    int stored();

    int capacity();

    /** Restores the stored energy (energy APIs with transactions roll back aborted inserts with it). */
    void restoreStored(int energy);
}
