package de.mcpj.fabric.network;

import de.mcpj.fabric.config.FabricConfigs;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.network.JumperActionPacket;
import de.mcpj.mc.network.JumperImpactPacket;
import de.mcpj.mc.network.PilotSoundPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/** MCPJ's messages on Fabric's networking API; the server also sends its settings to joining players. */
public final class FabricNetwork {
    private FabricNetwork() {}

    /** The server's settings for the client (the flight model runs on the pilot's client; NeoForge syncs them itself). */
    public record ServerConfigPacket(Map<String, String> values) implements CustomPacketPayload {
        public static final Type<ServerConfigPacket> TYPE = new Type<>(Mcpj.id("server_config"));
        public static final StreamCodec<FriendlyByteBuf, ServerConfigPacket> STREAM_CODEC = ByteBufCodecs
                .<FriendlyByteBuf, String, String, Map<String, String>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8)
                .map(ServerConfigPacket::new, ServerConfigPacket::values);

        @Override
        public Type<ServerConfigPacket> type() {
            return TYPE;
        }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(JumperActionPacket.TYPE, JumperActionPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(JumperImpactPacket.TYPE, JumperImpactPacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PilotSoundPacket.TYPE, PilotSoundPacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerConfigPacket.TYPE, ServerConfigPacket.STREAM_CODEC);

        // handlers run on the server thread
        ServerPlayNetworking.registerGlobalReceiver(JumperActionPacket.TYPE, (packet, context) -> packet.handle(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(JumperImpactPacket.TYPE, (packet, context) -> packet.handle(context.player()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sender.sendPacket(new ServerConfigPacket(FabricConfigs.SERVER.values())));
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        ServerPlayNetworking.send(player, (CustomPacketPayload) message);
    }
}
