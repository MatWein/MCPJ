package de.mcpj.fabric;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

/** MCPJ for Minecraft 1.20.1 on Fabric. */
public final class McpjFabric implements ModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("MCPJ (Fabric) initializing");
    }
}
