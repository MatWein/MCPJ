package de.mcpj.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.fabric.network.FabricNetwork;
import de.mcpj.mc.client.input.KeyBindings;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;

/** Client-only parts of the Fabric platform (kept apart so a dedicated server never loads client classes). */
public final class FabricClientServices {
    private FabricClientServices() {}

    public static KeyMapping keyMapping(String name, int defaultKey, boolean onlyWhilePiloting) {
        // keys that may share a key with vanilla (Tab, Q, E, I, L): Fabric has no conflict contexts, so they never
        // take the key over (see KeyMappingMixin); vanilla keys keep working and ours are read as raw keys
        return SharedKeyMapping.create("key.mcpj." + name, defaultKey, KeyBindings.CATEGORY, onlyWhilePiloting);
    }

    public static InputConstants.Key boundKey(KeyMapping key) {
        return KeyBindingHelper.getBoundKeyOf(key);
    }

    public static void sendToServer(Object message) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        ClientPlayNetworking.send(FabricNetwork.encode(message, buffer), buffer);
    }
}
