package de.mcpj.mc.client.input;

import de.mcpj.core.jumper.FlightInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class FlightInputReader {
    private FlightInputReader() {}

    public static FlightInput read() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.screen() != null) return FlightInput.NONE;
        float vertical = axis(minecraft.options.keyJump.isDown(), minecraft.options.keySprint.isDown());
        float strafe = axis(KeyBindings.isPhysicallyDown(KeyBindings.STRAFE_LEFT), KeyBindings.isPhysicallyDown(KeyBindings.STRAFE_RIGHT));
        return new FlightInput(player.input.getMoveVector().y, player.input.getMoveVector().x, vertical, strafe);
    }

    private static float axis(boolean positive, boolean negative) {
        return (positive ? 1f : 0f) - (negative ? 1f : 0f);
    }
}
