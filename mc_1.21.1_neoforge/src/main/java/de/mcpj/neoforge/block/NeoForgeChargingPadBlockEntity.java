package de.mcpj.neoforge.block;

import net.neoforged.neoforge.energy.IEnergyStorage;
import de.mcpj.core.pad.ChargingPad;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.neoforge.registry.NeoForgeObjects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The charging pad on NeoForge: energy goes in from all sides through NeoForge's energy capability. */
public class NeoForgeChargingPadBlockEntity extends ChargingPadBlockEntity {
    private final IEnergyStorage energy = new InputEnergyStorage(pad());

    public NeoForgeChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(NeoForgeObjects.CHARGING_PAD_ENTITY.get(), pos, state);
    }

    public ChargingPad logic() {
        return pad();
    }

    public IEnergyStorage energy() {
        return energy;
    }
}
