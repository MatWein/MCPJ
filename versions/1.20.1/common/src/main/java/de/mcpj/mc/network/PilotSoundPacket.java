package de.mcpj.mc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Consumer;

/**
 * A ship sound for its pilot, played without distance attenuation: the chase camera (the listener) is often
 * further away from the ship than a positional sound carries.
 */
public record PilotSoundPacket(SoundEvent sound, float volume, float pitch) {
    /** Set on the client (ClientSetup); plays the sound in the "cockpit". */
    public static Consumer<PilotSoundPacket> player = packet -> {};

    public static void encode(PilotSoundPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.sound.getLocation());
        buffer.writeFloat(packet.volume);
        buffer.writeFloat(packet.pitch);
    }

    public static PilotSoundPacket decode(FriendlyByteBuf buffer) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(buffer.readResourceLocation());
        return new PilotSoundPacket(sound, buffer.readFloat(), buffer.readFloat());
    }

    /** Client: plays the sound in the cockpit (the loader calls this on the client thread). */
    public void handle() {
        if (sound != null) player.accept(this);
    }
}
