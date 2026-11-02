package de.mcpj.forge.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.forge.client.PilotContext;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class KeyBindings {
    private static final String CATEGORY = "key.categories.mcpj";

    /** Active only while piloting, so keys like Tab, Q, E, I and L keep their vanilla meaning elsewhere. */
    private static final IKeyConflictContext WHILE_PILOTING = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return PilotContext.isPiloting();
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this;
        }
    };

    public static final KeyMapping CLOAK = key("cloak", GLFW.GLFW_KEY_C, KeyConflictContext.IN_GAME);
    public static final KeyMapping SHIELD = key("shield", GLFW.GLFW_KEY_V, KeyConflictContext.IN_GAME);
    public static final KeyMapping PODS = key("pods", GLFW.GLFW_KEY_G, KeyConflictContext.IN_GAME);
    public static final KeyMapping TARGET = key("target", GLFW.GLFW_KEY_TAB, WHILE_PILOTING);
    public static final KeyMapping STRAFE_LEFT = key("strafe_left", GLFW.GLFW_KEY_Q, WHILE_PILOTING);
    public static final KeyMapping STRAFE_RIGHT = key("strafe_right", GLFW.GLFW_KEY_E, WHILE_PILOTING);
    public static final KeyMapping CARGO = key("cargo", GLFW.GLFW_KEY_I, WHILE_PILOTING);
    public static final KeyMapping LIGHTS = key("lights", GLFW.GLFW_KEY_L, WHILE_PILOTING);

    public static final List<KeyMapping> ALL = List.of(CLOAK, SHIELD, PODS, TARGET, STRAFE_LEFT, STRAFE_RIGHT, CARGO, LIGHTS);

    private KeyBindings() {}

    private static KeyMapping key(String name, int defaultKey, IKeyConflictContext context) {
        return new KeyMapping("key.mcpj." + name, context, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    public static boolean isPhysicallyDown(KeyMapping key) {
        InputConstants.Key bound = key.getKey();
        return bound.getType() == InputConstants.Type.KEYSYM
                && InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), bound.getValue());
    }

    public static boolean matches(KeyMapping key, int glfwKey) {
        return key.getKey().getValue() == glfwKey;
    }

    public static void discardClicks(KeyMapping key) {
        boolean pending = key.consumeClick();
        while (pending) pending = key.consumeClick();
    }
}
