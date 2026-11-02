package de.mcpj.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** A key binding that may share its key with a vanilla one; it never takes the key over (see KeyMappingMixin). */
public final class SharedKeyMapping extends KeyMapping {
    private SharedKeyMapping(String name, int defaultKey, String category) {
        super(name, InputConstants.Type.KEYSYM, defaultKey, category);
    }

    static KeyMapping create(String name, int defaultKey, String category, boolean shared) {
        return shared ? new SharedKeyMapping(name, defaultKey, category) : new KeyMapping(name, InputConstants.Type.KEYSYM, defaultKey, category);
    }

    public static boolean isShared(KeyMapping key) {
        return key instanceof SharedKeyMapping;
    }
}
