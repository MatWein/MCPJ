package de.matthias.puddlejumper.registry;

import de.matthias.puddlejumper.PuddleJumperMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, PuddleJumperMod.MOD_ID);

    public static final RegistryObject<SoundEvent> ENGINE = SOUNDS.register("engine",
            () -> SoundEvent.createVariableRangeEvent(PuddleJumperMod.id("engine")));

    private ModSounds() {}
}
