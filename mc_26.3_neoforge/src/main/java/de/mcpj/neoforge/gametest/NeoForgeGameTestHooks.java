package de.mcpj.neoforge.gametest;

import de.mcpj.neoforge.block.NeoForgeChargingPadBlockEntity;
import de.mcpj.mc.gametest.GameTestHooks;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** The shared game tests on NeoForge: NeoForge's travel event and the pad's energy capability. */
public final class NeoForgeGameTestHooks implements GameTestHooks {
    @Override
    public boolean mayTravelAlone(Entity entity, ResourceKey<Level> destination) {
        return CommonHooks.onTravelToDimension(entity, destination);
    }

    @Override
    public int fillChargingPad(BlockEntity pad, int maxPerInsert) {
        EnergyHandler energy = pad.getLevel().getCapability(Capabilities.Energy.BLOCK, pad.getBlockPos(), Direction.UP);
        if (energy == null) throw new IllegalStateException("Charging pad has no energy capability");
        int inserted;
        do {
            try (Transaction transaction = Transaction.openRoot()) {
                inserted = energy.insert(maxPerInsert, transaction);
                transaction.commit();
            }
        } while (inserted > 0);
        return ((NeoForgeChargingPadBlockEntity) pad).logic().stored();
    }
}
