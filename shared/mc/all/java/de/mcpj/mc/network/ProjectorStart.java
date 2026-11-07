package de.mcpj.mc.network;

import de.mcpj.mc.block.JumperProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Server side of ProjectorStartPacket (the same for every Minecraft version). */
final class ProjectorStart {
    /** Players must stand near the projector (like for opening a container). */
    private static final double MAX_DISTANCE_SQR = 8 * 8;

    private ProjectorStart() {}

    static void handle(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE_SQR) return;
        if (!player.level().isLoaded(pos)) return;
        if (player.level().getBlockEntity(pos) instanceof JumperProjectorBlockEntity projector) projector.startByPlayer(player);
    }
}
