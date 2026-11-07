package de.mcpj.neoforge.gametest;

import net.neoforged.neoforge.energy.IEnergyStorage;
import de.mcpj.neoforge.block.NeoForgeChargingPadBlockEntity;
import de.mcpj.mc.gametest.GameTestHooks;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;

/** The shared game tests on NeoForge: NeoForge's travel event and the pad's energy capability. */
public final class NeoForgeGameTestHooks implements GameTestHooks {
    @Override
    public boolean mayTravelAlone(Entity entity, ResourceKey<Level> destination) {
        return CommonHooks.onTravelToDimension(entity, destination);
    }

    @Override
    public int fillChargingPad(BlockEntity pad, int maxPerInsert) {
        IEnergyStorage energy = pad.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, pad.getBlockPos(), Direction.UP);
        if (energy == null) throw new IllegalStateException("Charging pad has no energy capability");
        while (energy.receiveEnergy(maxPerInsert, false) > 0) {
            // keep inserting until the buffer is full
        }
        return ((NeoForgeChargingPadBlockEntity) pad).logic().stored();
    }
}
