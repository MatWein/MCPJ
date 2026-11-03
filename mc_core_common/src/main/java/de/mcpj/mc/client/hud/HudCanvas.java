package de.mcpj.mc.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** The few drawing calls the HUD needs; each Minecraft version adapts its own GUI graphics (see ClientCompat). */
public interface HudCanvas {
    void fill(int x1, int y1, int x2, int y2, int argb);

    /** Draws text with shadow; the color is RGB (no alpha). */
    void text(Font font, Component text, int x, int y, int rgb);
}
