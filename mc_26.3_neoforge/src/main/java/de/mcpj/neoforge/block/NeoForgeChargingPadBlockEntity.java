package de.mcpj.neoforge.block;

import de.mcpj.core.pad.ChargingPad;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.neoforge.registry.NeoForgeObjects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/** The charging pad on NeoForge: energy goes in from all sides through NeoForge's energy handler capability. */
public class NeoForgeChargingPadBlockEntity extends ChargingPadBlockEntity {
    private final EnergyHandler energy = new InputEnergyHandler(pad());

    public NeoForgeChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(NeoForgeObjects.CHARGING_PAD_ENTITY.get(), pos, state);
    }

    public ChargingPad logic() {
        return pad();
    }

    public EnergyHandler energy() {
        return energy;
    }
}
