package de.mcpj.mc.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.mc.platform.McpjPlatform;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class KeyBindings {
    public static final KeyMapping CLOAK = key("cloak", GLFW.GLFW_KEY_C, false);
    public static final KeyMapping SHIELD = key("shield", GLFW.GLFW_KEY_V, false);
    public static final KeyMapping PODS = key("pods", GLFW.GLFW_KEY_G, false);
    public static final KeyMapping TARGET = key("target", GLFW.GLFW_KEY_TAB, true);
    public static final KeyMapping STRAFE_LEFT = key("strafe_left", GLFW.GLFW_KEY_Q, true);
    public static final KeyMapping STRAFE_RIGHT = key("strafe_right", GLFW.GLFW_KEY_E, true);
    public static final KeyMapping CARGO = key("cargo", GLFW.GLFW_KEY_I, true);
    public static final KeyMapping LIGHTS = key("lights", GLFW.GLFW_KEY_L, true);
    public static final KeyMapping NAVIGATION = key("navigation", GLFW.GLFW_KEY_N, false);

    public static final List<KeyMapping> ALL = List.of(CLOAK, SHIELD, PODS, TARGET, STRAFE_LEFT, STRAFE_RIGHT, CARGO, LIGHTS, NAVIGATION);

    private KeyBindings() {}

    /** Keys active only while piloting may share a key with vanilla (Tab, Q, E, I, L keep their meaning elsewhere). */
    private static KeyMapping key(String name, int defaultKey, boolean onlyWhilePiloting) {
        return McpjPlatform.get().keyMapping(name, defaultKey, onlyWhilePiloting);
    }

    public static final String CATEGORY = "key.categories.mcpj";

    /** The key currently bound (players can rebind it). */
    public static InputConstants.Key boundKey(KeyMapping key) {
        return McpjPlatform.get().boundKey(key);
    }

    public static boolean isPhysicallyDown(KeyMapping key) {
        InputConstants.Key bound = boundKey(key);
        return bound.getType() == InputConstants.Type.KEYSYM
                && InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), bound.getValue());
    }

    public static boolean matches(KeyMapping key, int glfwKey) {
        return boundKey(key).getValue() == glfwKey;
    }

    public static void discardClicks(KeyMapping key) {
        boolean pending = key.consumeClick();
        while (pending) pending = key.consumeClick();
    }
}
