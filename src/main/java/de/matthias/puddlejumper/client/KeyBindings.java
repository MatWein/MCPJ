package de.matthias.puddlejumper.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static final String CATEGORY = "key.categories.puddlejumper";

    /** Nur aktiv, wenn man einen Jumper steuert - so bleibt Tab außerhalb die normale Spielerliste. */
    public static final IKeyConflictContext IN_JUMPER = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return ClientState.pilotedJumper() != null;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this;
        }
    };

    public static final KeyMapping CLOAK = key("key.puddlejumper.cloak", GLFW.GLFW_KEY_C, KeyConflictContext.IN_GAME);
    public static final KeyMapping SHIELD = key("key.puddlejumper.shield", GLFW.GLFW_KEY_V, KeyConflictContext.IN_GAME);
    public static final KeyMapping PODS = key("key.puddlejumper.pods", GLFW.GLFW_KEY_G, KeyConflictContext.IN_GAME);
    public static final KeyMapping TARGET = key("key.puddlejumper.target", GLFW.GLFW_KEY_TAB, IN_JUMPER);

    private KeyBindings() {}

    private static KeyMapping key(String name, int code, IKeyConflictContext ctx) {
        return new KeyMapping(name, ctx, InputConstants.Type.KEYSYM, code, CATEGORY);
    }
}
