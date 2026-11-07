package de.mcpj.mc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** A player confirmed starting a build at the jumper projector at {@code pos}. */
public record ProjectorStartPacket(BlockPos pos) {

    public static void encode(ProjectorStartPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static ProjectorStartPacket decode(FriendlyByteBuf buffer) {
        return new ProjectorStartPacket(buffer.readBlockPos());
    }

    /** Server: starts the build if the player can reach the projector. */
    public void handle(ServerPlayer sender) {
        ProjectorStart.handle(sender, pos);
    }
}
