package de.mcpj.neoforge.block;

import de.mcpj.neoforge.registry.NeoForgeObjects;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** NeoForge's energy API for Minecraft 1.21.1: charging pad and projector take energy from all sides. */
public final class NeoForgeEnergy {
    private NeoForgeEnergy() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, NeoForgeObjects.CHARGING_PAD_ENTITY.get(), (pad, side) -> pad.energy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, NeoForgeObjects.JUMPER_PROJECTOR_ENTITY.get(),
                (projector, side) -> new InputEnergyStorage(projector.logic()));
    }
}
