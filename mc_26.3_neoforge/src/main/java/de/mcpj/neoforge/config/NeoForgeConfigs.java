package de.mcpj.neoforge.config;

import de.mcpj.core.config.ClientConfig;
import de.mcpj.core.config.ServerConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Builds NeoForge config specs from the core's settings; register them with the mod loading context. */
public final class NeoForgeConfigs {
    public static final ModConfigSpec SERVER;
    public static final ModConfigSpec CLIENT;

    static {
        NeoForgeConfigBuilder server = new NeoForgeConfigBuilder();
        ServerConfig.define(server);
        SERVER = server.build();
        NeoForgeConfigBuilder client = new NeoForgeConfigBuilder();
        ClientConfig.define(client);
        CLIENT = client.build();
    }

    private NeoForgeConfigs() {}
}
