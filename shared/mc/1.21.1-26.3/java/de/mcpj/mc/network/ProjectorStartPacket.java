package de.mcpj.mc.network;

import de.mcpj.mc.Mcpj;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** A player confirmed starting a build at the jumper projector at {@code pos}. */
public record ProjectorStartPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ProjectorStartPacket> TYPE = new Type<>(Mcpj.id("projector_start"));
    public static final StreamCodec<FriendlyByteBuf, ProjectorStartPacket> STREAM_CODEC = StreamCodec.ofMember(ProjectorStartPacket::encode, ProjectorStartPacket::decode);

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

    @Override
    public Type<ProjectorStartPacket> type() {
        return TYPE;
    }
}
