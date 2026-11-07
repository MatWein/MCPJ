package de.mcpj.mc.network;

import de.mcpj.core.navigation.JumperNavigation;
import de.mcpj.core.navigation.NavigationAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** A change made in the navigation console (see JumperNavigation). */
public record NavigationPacket(NavigationAction action, int index, String name) {

    public static void encode(NavigationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeVarInt(packet.index);
        buffer.writeUtf(packet.name, JumperNavigation.MAX_NAME_LENGTH * 2);
    }

    public static NavigationPacket decode(FriendlyByteBuf buffer) {
        return new NavigationPacket(buffer.readEnum(NavigationAction.class), buffer.readVarInt(), buffer.readUtf(JumperNavigation.MAX_NAME_LENGTH * 2));
    }

    /** Server: applies the change to the sender's jumper. */
    public void handle(ServerPlayer sender) {
        NavigationEdit.handle(sender, action, index, name);
    }
}
