package de.matthias.puddlejumper.client.sound;

import de.matthias.puddlejumper.network.PilotSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Ship sounds for the pilot: relative to the listener and without attenuation, so the chase camera always hears them. */
public final class PilotSounds {
    private PilotSounds() {}

    public static void play(PilotSoundPacket packet) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(packet.sound().getLocation(), SoundSource.NEUTRAL,
                packet.volume(), packet.pitch(), SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }
}
