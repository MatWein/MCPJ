package de.mcpj.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.mc.client.input.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Client-only parts of the NeoForge platform for Minecraft 26.3 (a dedicated server never loads this class). */
public final class NeoForgeClientServices {
    private NeoForgeClientServices() {}

    /** Minecraft 26.3 lets several bindings share a key, so ours (e.g. C, Tab, Q, E) leave vanilla's working. */
    public static KeyMapping keyMapping(String name, int defaultKey, boolean onlyWhilePiloting) {
        return new KeyMapping("key.mcpj." + name, InputConstants.Type.KEYBOARD, defaultKey, KeyBindings.CATEGORY);
    }

    public static void sendToServer(Object message) {
        ClientPacketDistributor.sendToServer((CustomPacketPayload) message);
    }
}
