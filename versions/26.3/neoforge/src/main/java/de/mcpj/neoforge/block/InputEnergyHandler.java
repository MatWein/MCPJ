package de.mcpj.neoforge.block;

import de.mcpj.core.energy.EnergyInput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * NeoForge energy handler of a block that takes energy (charging pad, projector): insert only; transactions roll
 * back by restoring the stored energy.
 */
public final class InputEnergyHandler extends SnapshotJournal<Integer> implements EnergyHandler {
    private final EnergyInput input;

    public InputEnergyHandler(EnergyInput input) {
        this.input = input;
    }

    @Override
    public long getAmountAsLong() {
        return input.stored();
    }

    @Override
    public long getCapacityAsLong() {
        return input.capacity();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        int accepted = input.receive(amount, true);
        if (accepted <= 0) return 0;
        updateSnapshots(transaction);
        return input.receive(accepted, false);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    protected Integer createSnapshot() {
        return input.stored();
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        input.restoreStored(snapshot);
    }
}
