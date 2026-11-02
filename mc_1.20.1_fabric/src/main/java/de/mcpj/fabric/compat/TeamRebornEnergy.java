package de.mcpj.fabric.compat;

import de.mcpj.core.pad.ChargingPad;
import de.mcpj.fabric.registry.FabricObjects;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/**
 * Team Reborn Energy (the energy API of Fabric's tech mods): charging pads accept energy from all sides.
 * Only loaded when the API is installed; one unit counts as one Forge Energy unit.
 */
public final class TeamRebornEnergy {
    private TeamRebornEnergy() {}

    public static void register() {
        EnergyStorage.SIDED.registerForBlockEntity((pad, side) -> new PadStorage(pad.logic()), FabricObjects.CHARGING_PAD_ENTITY);
    }

    /** Insert-only view of a pad; aborted transactions restore the stored energy. */
    private static final class PadStorage extends SnapshotParticipant<Integer> implements EnergyStorage {
        private final ChargingPad pad;

        PadStorage(ChargingPad pad) {
            this.pad = pad;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            int accepted = pad.receive((int) Math.min(maxAmount, Integer.MAX_VALUE), true);
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            return pad.receive(accepted, false);
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long getAmount() {
            return pad.stored();
        }

        @Override
        public long getCapacity() {
            return pad.capacity();
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        protected Integer createSnapshot() {
            return pad.stored();
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            pad.restoreStored(snapshot);
        }
    }
}
