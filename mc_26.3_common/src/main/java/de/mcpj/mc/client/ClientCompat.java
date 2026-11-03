package de.mcpj.mc.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.mc.client.hud.HudCanvas;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** Client-side counterpart of McCompat: the same methods in every mc_&lt;version&gt;_common. */
public final class ClientCompat {
    /** Action of a raw key event: pressed. */
    public static final int KEY_PRESS = InputConstants.PRESS;

    private ClientCompat() {}

    public static boolean screenOpen(Minecraft minecraft) {
        return minecraft.gui.screen() != null;
    }

    public static boolean hudHidden(Minecraft minecraft) {
        return minecraft.gui.hud.isHidden();
    }

    @Nullable
    public static Entity cameraEntity(Camera camera) {
        return camera.entity();
    }

    public static float forwardImpulse(LocalPlayer player) {
        return player.input.getMoveVector().y;
    }

    public static float leftImpulse(LocalPlayer player) {
        return player.input.getMoveVector().x;
    }

    public static Identifier soundId(SoundEvent sound) {
        return sound.location();
    }

    public static HudCanvas hudCanvas(GuiGraphicsExtractor graphics) {
        return new HudCanvas() {
            @Override
            public void fill(int x1, int y1, int x2, int y2, int argb) {
                graphics.fill(x1, y1, x2, y2, argb);
            }

            @Override
            public void text(Font font, Component text, int x, int y, int rgb) {
                graphics.text(font, text, x, y, ARGB.opaque(rgb), true);
            }
        };
    }
}
