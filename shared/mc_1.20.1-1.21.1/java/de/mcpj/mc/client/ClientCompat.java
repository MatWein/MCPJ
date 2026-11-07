package de.mcpj.mc.client;

import de.mcpj.mc.client.hud.HudCanvas;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** Client-side counterpart of McCompat: the same methods in every mc_&lt;version&gt;_common. */
public final class ClientCompat {
    /** Action of a raw key event: pressed. */
    public static final int KEY_PRESS = GLFW.GLFW_PRESS;

    private ClientCompat() {}

    public static boolean screenOpen(Minecraft minecraft) {
        return minecraft.screen != null;
    }

    public static boolean hudHidden(Minecraft minecraft) {
        return minecraft.options.hideGui;
    }

    @Nullable
    public static Entity cameraEntity(Camera camera) {
        return camera.getEntity();
    }

    public static float forwardImpulse(LocalPlayer player) {
        return player.input.forwardImpulse;
    }

    public static float leftImpulse(LocalPlayer player) {
        return player.input.leftImpulse;
    }

    public static ResourceLocation soundId(SoundEvent sound) {
        return sound.getLocation();
    }

    public static HudCanvas hudCanvas(GuiGraphics graphics) {
        return new HudCanvas() {
            @Override
            public void fill(int x1, int y1, int x2, int y2, int argb) {
                graphics.fill(x1, y1, x2, y2, argb);
            }

            @Override
            public void text(Font font, Component text, int x, int y, int rgb) {
                graphics.drawString(font, text, x, y, rgb, true);
            }
        };
    }
}
