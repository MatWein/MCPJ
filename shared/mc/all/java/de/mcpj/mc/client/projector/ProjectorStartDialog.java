package de.mcpj.mc.client.projector;

import de.mcpj.mc.client.ClientCompat;
import de.mcpj.mc.network.ProjectorStartPacket;
import de.mcpj.mc.platform.McpjPlatform;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Yes/no dialog after right-clicking a jumper projector's top: on yes the server starts the build. */
public final class ProjectorStartDialog {
    private ProjectorStartDialog() {}

    public static void open(BlockPos pos) {
        ClientCompat.setScreen(new ConfirmScreen(confirmed -> {
            ClientCompat.setScreen(null);
            if (confirmed) McpjPlatform.get().sendToServer(new ProjectorStartPacket(pos));
        }, Component.translatable("block.mcpj.jumper_projector"), Component.translatable("message.mcpj.projector_confirm")));
    }
}
