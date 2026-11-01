package de.matthias.puddlejumper.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static final String CATEGORY = "key.categories.puddlejumper";
    public static final KeyMapping CLOAK = key("key.puddlejumper.cloak", GLFW.GLFW_KEY_C);
    public static final KeyMapping HATCH = key("key.puddlejumper.hatch", GLFW.GLFW_KEY_H);
    public static final KeyMapping PODS = key("key.puddlejumper.pods", GLFW.GLFW_KEY_G);

    private KeyBindings() {}

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }
}
