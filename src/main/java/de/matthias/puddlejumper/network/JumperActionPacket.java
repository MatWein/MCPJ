package de.matthias.puddlejumper.network;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Server: Pilot löst eine Aktion aus (Tarnung, Gondeln, Drohne). */
public record JumperActionPacket(Action action, int targetId) {

    public enum Action { CLOAK, PODS, FIRE }

    public static void encode(JumperActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.targetId);
    }

    public static JumperActionPacket decode(FriendlyByteBuf buf) {
        return new JumperActionPacket(buf.readEnum(Action.class), buf.readVarInt());
    }

    public static void handle(JumperActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.getVehicle() instanceof PuddleJumperEntity jumper) {
            jumper.handleAction(player, msg.action, msg.targetId);
        }
        ctx.get().setPacketHandled(true);
    }
}
