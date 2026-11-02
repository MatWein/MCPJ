package de.mcpj.network;

import de.mcpj.entity.jumper.JumperAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record JumperActionPacket(JumperAction action, int targetId) {
    public static final int NO_TARGET = -1;

    public static void encode(JumperActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeVarInt(packet.targetId);
    }

    public static JumperActionPacket decode(FriendlyByteBuf buffer) {
        return new JumperActionPacket(buffer.readEnum(JumperAction.class), buffer.readVarInt());
    }

    public static void handle(JumperActionPacket packet, Supplier<NetworkEvent.Context> context) {
        ModNetwork.handleFromPilot(context, (jumper, pilot) -> jumper.systems().perform(pilot, packet.action, packet.targetId));
    }
}
