package de.mcpj.mc.network;

import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.Mcpj;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Consumer;

/**
 * A ship sound for its pilot, played without distance attenuation: the chase camera (the listener) is often
 * further away from the ship than a positional sound carries.
 */
public record PilotSoundPacket(SoundEvent sound, float volume, float pitch) implements CustomPacketPayload {
    public static final Type<PilotSoundPacket> TYPE = new Type<>(Mcpj.id("pilot_sound"));
    public static final StreamCodec<FriendlyByteBuf, PilotSoundPacket> STREAM_CODEC = StreamCodec.ofMember(PilotSoundPacket::encode, PilotSoundPacket::decode);

    /** Set on the client (ClientSetup); plays the sound in the "cockpit". */
    public static Consumer<PilotSoundPacket> player = packet -> {};

    public static void encode(PilotSoundPacket packet, FriendlyByteBuf buffer) {
        McCompat.writeSound(buffer, packet.sound);
        buffer.writeFloat(packet.volume);
        buffer.writeFloat(packet.pitch);
    }

    public static PilotSoundPacket decode(FriendlyByteBuf buffer) {
        SoundEvent sound = McCompat.readSound(buffer);
        return new PilotSoundPacket(sound, buffer.readFloat(), buffer.readFloat());
    }

    /** Client: plays the sound in the cockpit (the loader calls this on the client thread). */
    public void handle() {
        if (sound != null) player.accept(this);
    }

    @Override
    public Type<PilotSoundPacket> type() {
        return TYPE;
    }
}
