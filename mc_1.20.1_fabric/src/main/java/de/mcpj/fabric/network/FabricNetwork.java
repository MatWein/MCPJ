package de.mcpj.fabric.network;

import de.mcpj.fabric.config.FabricConfigs;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.network.JumperActionPacket;
import de.mcpj.mc.network.JumperImpactPacket;
import de.mcpj.mc.network.PilotSoundPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** MCPJ's messages on Fabric's networking API; the server also sends its settings to joining players. */
public final class FabricNetwork {
    public static final ResourceLocation ACTION = Mcpj.id("action");
    public static final ResourceLocation IMPACT = Mcpj.id("impact");
    public static final ResourceLocation PILOT_SOUND = Mcpj.id("pilot_sound");
    public static final ResourceLocation SERVER_CONFIG = Mcpj.id("server_config");

    private FabricNetwork() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, sender) -> {
            JumperActionPacket packet = JumperActionPacket.decode(buf);
            server.execute(() -> packet.handle(player));
        });
        ServerPlayNetworking.registerGlobalReceiver(IMPACT, (server, player, handler, buf, sender) -> {
            JumperImpactPacket packet = JumperImpactPacket.decode(buf);
            server.execute(() -> packet.handle(player));
        });
        // the flight model runs on the pilot's client: it needs the server's settings (Forge syncs them itself)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sender.sendPacket(SERVER_CONFIG, encodeConfig(FabricConfigs.SERVER.values())));
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        ServerPlayNetworking.send(player, encode(message, buffer), buffer);
    }

    /** Writes the message and returns its channel. */
    public static ResourceLocation encode(Object message, FriendlyByteBuf buffer) {
        if (message instanceof JumperActionPacket action) {
            JumperActionPacket.encode(action, buffer);
            return ACTION;
        }
        if (message instanceof JumperImpactPacket impact) {
            JumperImpactPacket.encode(impact, buffer);
            return IMPACT;
        }
        if (message instanceof PilotSoundPacket sound) {
            PilotSoundPacket.encode(sound, buffer);
            return PILOT_SOUND;
        }
        throw new IllegalArgumentException("Unknown message " + message);
    }

    private static FriendlyByteBuf encodeConfig(Map<String, String> values) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeMap(values, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeUtf);
        return buffer;
    }

    public static Map<String, String> decodeConfig(FriendlyByteBuf buffer) {
        return buffer.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readUtf);
    }
}
