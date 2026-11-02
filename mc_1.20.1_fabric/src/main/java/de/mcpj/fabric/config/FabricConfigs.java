package de.mcpj.fabric.config;

import de.mcpj.core.config.ClientConfig;
import de.mcpj.core.config.ServerConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** The server and client settings as files in Fabric's config folder (mcpj-server.toml, mcpj-client.toml). */
public final class FabricConfigs {
    public static final TomlConfig SERVER = new TomlConfig(dir().resolve("mcpj-server.toml"));
    public static final TomlConfig CLIENT = new TomlConfig(dir().resolve("mcpj-client.toml"));

    private FabricConfigs() {}

    public static void define() {
        ServerConfig.define(SERVER);
        ClientConfig.define(CLIENT);
        SERVER.load();
    }

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
