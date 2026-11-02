package de.matthias.puddlejumper.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;
import java.util.function.Supplier;

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
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(buffer.readResourceLocation());
        return new PilotSoundPacket(sound, buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(PilotSoundPacket packet, Supplier<NetworkEvent.Context> context) {
        if (packet.sound != null) context.get().enqueueWork(() -> player.accept(packet));
        context.get().setPacketHandled(true);
    }
}
