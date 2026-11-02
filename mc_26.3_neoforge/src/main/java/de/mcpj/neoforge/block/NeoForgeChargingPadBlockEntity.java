package de.mcpj.neoforge.block;

import de.mcpj.core.pad.ChargingPad;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.neoforge.registry.NeoForgeObjects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** The charging pad on NeoForge: energy goes in from all sides through NeoForge's energy handler capability. */
public class NeoForgeChargingPadBlockEntity extends ChargingPadBlockEntity {
    private final PadEnergy energy = new PadEnergy();

    public NeoForgeChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(NeoForgeObjects.CHARGING_PAD_ENTITY.get(), pos, state);
    }

    public ChargingPad logic() {
        return pad();
    }

    public EnergyHandler energy() {
        return energy;
    }

    /** Insert-only energy handler; transactions roll back by restoring the stored energy. */
    private final class PadEnergy extends SnapshotJournal<Integer> implements EnergyHandler {
        @Override
        public long getAmountAsLong() {
            return pad().stored();
        }

        @Override
        public long getCapacityAsLong() {
            return pad().capacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int accepted = pad().receive(amount, true);
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            return pad().receive(accepted, false);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        protected Integer createSnapshot() {
            return pad().stored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            pad().restoreStored(snapshot);
        }
    }
}
