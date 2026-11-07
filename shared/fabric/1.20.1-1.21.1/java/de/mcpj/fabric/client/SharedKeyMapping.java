package de.mcpj.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** An MCPJ key binding: it may share its key with vanilla ones and never takes a key over (see KeyMappingMixin). */
public final class SharedKeyMapping extends KeyMapping {
    private SharedKeyMapping(String name, int defaultKey, String category) {
        super(name, InputConstants.Type.KEYSYM, defaultKey, category);
    }

    static KeyMapping create(String name, int defaultKey, String category) {
        return new SharedKeyMapping(name, defaultKey, category);
    }

    public static boolean isShared(KeyMapping key) {
        return key instanceof SharedKeyMapping;
    }
}
