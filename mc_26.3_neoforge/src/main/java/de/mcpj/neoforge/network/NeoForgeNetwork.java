package de.mcpj.neoforge.network;

import de.mcpj.mc.network.JumperActionPacket;
import de.mcpj.mc.network.JumperImpactPacket;
import de.mcpj.mc.network.PilotSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** MCPJ's messages on NeoForge (handlers run on the game thread); NeoForge syncs the server config itself. */
public final class NeoForgeNetwork {
    private static final String PROTOCOL = "1";

    private NeoForgeNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToServer(JumperActionPacket.TYPE, JumperActionPacket.STREAM_CODEC,
                (packet, context) -> packet.handle((ServerPlayer) context.player()));
        registrar.playToServer(JumperImpactPacket.TYPE, JumperImpactPacket.STREAM_CODEC,
                (packet, context) -> packet.handle((ServerPlayer) context.player()));
        registrar.playToClient(PilotSoundPacket.TYPE, PilotSoundPacket.STREAM_CODEC, (packet, context) -> packet.handle());
    }
}
