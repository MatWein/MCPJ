package de.mcpj.mc.network;

import de.mcpj.core.jumper.JumperAction;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record JumperActionPacket(JumperAction action, int targetId) {
    public static final int NO_TARGET = -1;

    public static void encode(JumperActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeVarInt(packet.targetId);
    }

    public static JumperActionPacket decode(FriendlyByteBuf buffer) {
        return new JumperActionPacket(buffer.readEnum(JumperAction.class), buffer.readVarInt());
    }

    /** Server: the sender's action, if they pilot a jumper. */
    public void handle(ServerPlayer sender) {
        if (sender.getVehicle() instanceof PuddleJumperEntity jumper && jumper.getControllingPassenger() == sender) {
            jumper.performPilotAction(sender, action, targetId);
        }
    }
}
