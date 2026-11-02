package de.mcpj.config;

import de.mcpj.core.config.ClientConfig;
import de.mcpj.core.config.ServerConfig;
import net.minecraftforge.common.ForgeConfigSpec;

/** Builds Forge config specs from the core's settings; register them with the mod loading context. */
public final class ForgeConfigs {
    public static final ForgeConfigSpec SERVER;
    public static final ForgeConfigSpec CLIENT;

    static {
        ForgeConfigBuilder server = new ForgeConfigBuilder();
        ServerConfig.define(server);
        SERVER = server.build();
        ForgeConfigBuilder client = new ForgeConfigBuilder();
        ClientConfig.define(client);
        CLIENT = client.build();
    }

    private ForgeConfigs() {}
}
