package de.mcpj.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.mc.client.PilotContext;
import de.mcpj.mc.client.input.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only parts of the NeoForge platform for Minecraft 1.21.1 (a dedicated server never loads this class). */
public final class NeoForgeClientServices {
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

    private NeoForgeClientServices() {}

    public static KeyMapping keyMapping(String name, int defaultKey, boolean onlyWhilePiloting) {
        return new KeyMapping("key.mcpj." + name, onlyWhilePiloting ? WHILE_PILOTING : KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, defaultKey, KeyBindings.CATEGORY);
    }

    public static void sendToServer(Object message) {
        PacketDistributor.sendToServer((CustomPacketPayload) message);
    }
}
