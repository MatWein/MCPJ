package de.mcpj.mc.network;

import de.mcpj.core.navigation.NavigationAction;
import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.util.Convert;
import net.minecraft.server.level.ServerPlayer;

/** Server side of NavigationPacket (the same for every Minecraft version). */
final class NavigationEdit {
    private NavigationEdit() {}

    /** Anyone on board may change the navigation memory of their jumper; new waypoints take its current position. */
    static void handle(ServerPlayer player, NavigationAction action, int index, String name) {
        if (!(player.getVehicle() instanceof PuddleJumperEntity jumper)) return;
        jumper.core().navigation().apply(action, index, name, Convert.toCore(jumper.blockPosition()), McCompat.dimensionId(jumper.level()));
    }
}
