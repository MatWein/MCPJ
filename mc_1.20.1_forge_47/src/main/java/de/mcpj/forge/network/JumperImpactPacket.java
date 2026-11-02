package de.mcpj.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record JumperImpactPacket(float horizontalSpeed, float fallSpeed) {

    public static void encode(JumperImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.horizontalSpeed);
        buffer.writeFloat(packet.fallSpeed);
    }

    public static JumperImpactPacket decode(FriendlyByteBuf buffer) {
        return new JumperImpactPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(JumperImpactPacket packet, Supplier<NetworkEvent.Context> context) {
        ModNetwork.handleFromPilot(context, (jumper, pilot) -> jumper.hull().onImpact(packet.horizontalSpeed, packet.fallSpeed));
    }
}
