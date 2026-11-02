package de.matthias.puddlejumper.network;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
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
            PuddleJumperMod.id("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(JumperActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JumperActionPacket::encode)
                .decoder(JumperActionPacket::decode)
                .consumerMainThread(JumperActionPacket::handle)
                .add();
        CHANNEL.messageBuilder(JumperImpactPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(JumperImpactPacket::encode)
                .decoder(JumperImpactPacket::decode)
                .consumerMainThread(JumperImpactPacket::handle)
                .add();
        CHANNEL.messageBuilder(PilotSoundPacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PilotSoundPacket::encode)
                .decoder(PilotSoundPacket::decode)
                .consumerNetworkThread(PilotSoundPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    static void handleFromPilot(Supplier<NetworkEvent.Context> context, BiConsumer<PuddleJumperEntity, ServerPlayer> handler) {
        ServerPlayer player = context.get().getSender();
        if (player != null && player.getVehicle() instanceof PuddleJumperEntity jumper && jumper.getControllingPassenger() == player) {
            handler.accept(jumper, player);
        }
        context.get().setPacketHandled(true);
    }
}
