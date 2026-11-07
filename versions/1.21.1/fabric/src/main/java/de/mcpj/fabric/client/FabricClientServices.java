package de.mcpj.fabric.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.mc.client.input.KeyBindings;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

/** Client-only parts of the Fabric platform (kept apart so a dedicated server never loads client classes). */
public final class FabricClientServices {
    private FabricClientServices() {}

    public static KeyMapping keyMapping(String name, int defaultKey, boolean onlyWhilePiloting) {
        // vanilla gives each key to one binding only and Fabric has no conflict contexts: our keys never take a key
        // over (see KeyMappingMixin), so vanilla keys (e.g. C = save hotbar, Tab, Q, E) keep working; MCPJ reads its
        // keys as raw key presses
        return SharedKeyMapping.create("key.mcpj." + name, defaultKey, KeyBindings.CATEGORY);
    }

    public static InputConstants.Key boundKey(KeyMapping key) {
        return KeyBindingHelper.getBoundKeyOf(key);
    }

    public static void sendToServer(Object message) {
        ClientPlayNetworking.send((CustomPacketPayload) message);
    }
}
