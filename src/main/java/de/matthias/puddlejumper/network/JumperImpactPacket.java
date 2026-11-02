package de.matthias.puddlejumper.network;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: Der Client des Piloten simuliert den Flug und meldet Aufpralle
 * (verlorenes Tempo in Blöcken/Tick, horizontal und nach unten). Der Server prüft und verbucht den Schaden.
 */
public record JumperImpactPacket(float horizontal, float down) {

    public static void encode(JumperImpactPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.horizontal);
        buf.writeFloat(msg.down);
    }

    public static JumperImpactPacket decode(FriendlyByteBuf buf) {
        return new JumperImpactPacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(JumperImpactPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.getVehicle() instanceof PuddleJumperEntity jumper && jumper.getControllingPassenger() == player) {
            jumper.onImpact(msg.horizontal, msg.down);
        }
        ctx.get().setPacketHandled(true);
    }
}
