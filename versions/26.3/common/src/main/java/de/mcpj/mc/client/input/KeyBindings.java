package de.mcpj.mc.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.Mcpj;
import net.minecraft.client.KeyMapping;

import java.util.List;

public final class KeyBindings {
    public static final KeyMapping CLOAK = key("cloak", InputConstants.KEY_C, false);
    public static final KeyMapping SHIELD = key("shield", InputConstants.KEY_V, false);
    public static final KeyMapping PODS = key("pods", InputConstants.KEY_G, false);
    public static final KeyMapping TARGET = key("target", InputConstants.KEY_TAB, true);
    public static final KeyMapping STRAFE_LEFT = key("strafe_left", InputConstants.KEY_Q, true);
    public static final KeyMapping STRAFE_RIGHT = key("strafe_right", InputConstants.KEY_E, true);
    public static final KeyMapping CARGO = key("cargo", InputConstants.KEY_I, true);
    public static final KeyMapping LIGHTS = key("lights", InputConstants.KEY_L, true);
    public static final KeyMapping NAVIGATION = key("navigation", InputConstants.KEY_N, false);

    public static final List<KeyMapping> ALL = List.of(CLOAK, SHIELD, PODS, TARGET, STRAFE_LEFT, STRAFE_RIGHT, CARGO, LIGHTS, NAVIGATION);

    private KeyBindings() {}

    /** Keys active only while piloting may share a key with vanilla (Tab, Q, E, I, L keep their meaning elsewhere). */
    private static KeyMapping key(String name, int defaultKey, boolean onlyWhilePiloting) {
        return McpjPlatform.get().keyMapping(name, defaultKey, onlyWhilePiloting);
    }

    /** Category of the mod's keys (translation key "key.category.mcpj.jumper"). */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Mcpj.id("jumper"));

    /** The key currently bound (players can rebind it). */
    public static InputConstants.Key boundKey(KeyMapping key) {
        return McpjPlatform.get().boundKey(key);
    }

    public static boolean isPhysicallyDown(KeyMapping key) {
        InputConstants.Key bound = boundKey(key);
        return bound.getType() == InputConstants.Type.KEYBOARD && InputConstants.isKeyDown(bound.getValue());
    }

    public static boolean matches(KeyMapping key, int scancode) {
        InputConstants.Key bound = boundKey(key);
        return bound.getType() == InputConstants.Type.KEYBOARD && bound.getValue() == scancode;
    }

    public static void discardClicks(KeyMapping key) {
        boolean pending = key.consumeClick();
        while (pending) pending = key.consumeClick();
    }
}
