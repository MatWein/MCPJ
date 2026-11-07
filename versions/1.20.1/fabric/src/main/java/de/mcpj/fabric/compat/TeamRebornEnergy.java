package de.mcpj.fabric.compat;

import de.mcpj.core.energy.EnergyInput;
import de.mcpj.fabric.registry.FabricObjects;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/**
 * Team Reborn Energy (the energy API of Fabric's tech mods): charging pads and projectors accept energy from all sides.
 * Only loaded when the API is installed; one unit counts as one Forge Energy unit.
 */
public final class TeamRebornEnergy {
    private TeamRebornEnergy() {}

    public static void register() {
        EnergyStorage.SIDED.registerForBlockEntity((pad, side) -> new InputStorage(pad.logic()), FabricObjects.CHARGING_PAD_ENTITY);
        EnergyStorage.SIDED.registerForBlockEntity((projector, side) -> new InputStorage(projector.logic()), FabricObjects.JUMPER_PROJECTOR_ENTITY);
    }

    /** Insert-only view of a pad or projector; aborted transactions restore the stored energy. */
    private static final class InputStorage extends SnapshotParticipant<Integer> implements EnergyStorage {
        private final EnergyInput input;

        InputStorage(EnergyInput input) {
            this.input = input;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            int accepted = input.receive((int) Math.min(maxAmount, Integer.MAX_VALUE), true);
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            return input.receive(accepted, false);
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long getAmount() {
            return input.stored();
        }

        @Override
        public long getCapacity() {
            return input.capacity();
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        protected Integer createSnapshot() {
            return input.stored();
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            input.restoreStored(snapshot);
        }
    }
}
