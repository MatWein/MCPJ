package de.mcpj.forge.registry;

import de.mcpj.mc.registry.McpjObjects;
import de.mcpj.forge.McpjMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, McpjMod.MOD_ID);

    public static final RegistryObject<SoundEvent> ENGINE = SOUNDS.register(McpjObjects.ENGINE, McpjObjects::engineSound);

    private ModSounds() {}
}
