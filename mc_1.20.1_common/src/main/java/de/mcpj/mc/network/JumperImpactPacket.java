package de.mcpj.mc.network;

import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record JumperImpactPacket(float horizontalSpeed, float fallSpeed) {

    public static void encode(JumperImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.horizontalSpeed);
        buffer.writeFloat(packet.fallSpeed);
    }

    public static JumperImpactPacket decode(FriendlyByteBuf buffer) {
        return new JumperImpactPacket(buffer.readFloat(), buffer.readFloat());
    }

    /** Server: a collision reported by the pilot's client. */
    public void handle(ServerPlayer sender) {
        if (sender.getVehicle() instanceof PuddleJumperEntity jumper && jumper.getControllingPassenger() == sender) {
            jumper.hull().onImpact(horizontalSpeed, fallSpeed);
        }
    }
}
