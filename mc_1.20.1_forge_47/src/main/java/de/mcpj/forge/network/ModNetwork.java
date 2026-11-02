package de.mcpj.forge.network;

import de.mcpj.mc.Mcpj;
import de.mcpj.mc.network.JumperActionPacket;
import de.mcpj.mc.network.JumperImpactPacket;
import de.mcpj.mc.network.PilotSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Mcpj.id("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(JumperActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JumperActionPacket::encode)
                .decoder(JumperActionPacket::decode)
                .consumerMainThread(fromPlayer(JumperActionPacket::handle))
                .add();
        CHANNEL.messageBuilder(JumperImpactPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JumperImpactPacket::encode)
                .decoder(JumperImpactPacket::decode)
                .consumerMainThread(fromPlayer(JumperImpactPacket::handle))
                .add();
        CHANNEL.messageBuilder(PilotSoundPacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PilotSoundPacket::encode)
                .decoder(PilotSoundPacket::decode)
                .consumerNetworkThread((packet, context) -> {
                    context.get().enqueueWork(packet::handle);
                    context.get().setPacketHandled(true);
                })
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    /** Server-bound message: handled on the main thread with its sender. */
    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> fromPlayer(BiConsumer<T, ServerPlayer> handler) {
        return (packet, context) -> {
            ServerPlayer sender = context.get().getSender();
            if (sender != null) handler.accept(packet, sender);
            context.get().setPacketHandled(true);
        };
    }
}
