package de.mcpj.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.mc.client.input.KeyBindings;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client-only parts of the Fabric platform (kept apart so a dedicated server never loads client classes). */
public final class FabricClientServices {
    private FabricClientServices() {}

    /** Minecraft 26.3 lets several bindings share a key, so ours (e.g. C, Tab, Q, E) leave vanilla's working. */
    public static KeyMapping keyMapping(String name, int defaultKey, boolean onlyWhilePiloting) {
        return new KeyMapping("key.mcpj." + name, InputConstants.Type.KEYBOARD, defaultKey, KeyBindings.CATEGORY);
    }

    public static InputConstants.Key boundKey(KeyMapping key) {
        return KeyMappingHelper.getBoundKeyOf(key);
    }

    public static void sendToServer(Object message) {
        ClientPlayNetworking.send((CustomPacketPayload) message);
    }
}
