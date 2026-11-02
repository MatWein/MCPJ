package de.mcpj.forge.client;

import de.mcpj.forge.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

public final class PilotContext {
    private PilotContext() {}

    @Nullable
    public static PuddleJumperEntity pilotedJumper() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof PuddleJumperEntity jumper && jumper.getControllingPassenger() == player) {
            return jumper;
        }
        return null;
    }

    public static boolean isPiloting() {
        return pilotedJumper() != null;
    }
}
