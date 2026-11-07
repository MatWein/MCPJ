package de.mcpj.neoforge;

import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import de.mcpj.mc.gametest.McpjGameTests;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.gametest.GameTestHooks;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.neoforge.config.NeoForgeConfigs;
import de.mcpj.neoforge.event.ServerEvents;
import de.mcpj.neoforge.gametest.NeoForgeGameTestHooks;
import de.mcpj.neoforge.network.NeoForgeNetwork;
import de.mcpj.neoforge.registry.NeoForgeObjects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/** MCPJ for Minecraft 1.21.1 on NeoForge. */
@Mod(Mcpj.MOD_ID)
public final class McpjNeoForge {
    public McpjNeoForge(IEventBus modBus, ModContainer container) {
        McpjPlatform.set(new NeoForgePlatform());
        GameTestHooks.set(new NeoForgeGameTestHooks());
        container.registerConfig(ModConfig.Type.SERVER, NeoForgeConfigs.SERVER, "mcpj-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, NeoForgeConfigs.CLIENT, "mcpj-client.toml");
        NeoForgeObjects.register(modBus);
        modBus.addListener(NeoForgeNetwork::register);
        ServerEvents.register(modBus, NeoForge.EVENT_BUS);
        modBus.addListener(RegisterGameTestsEvent.class, event -> event.register(McpjGameTests.class));
    }
}
