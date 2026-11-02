package de.mcpj.fabric;

import de.mcpj.fabric.compat.TeamRebornEnergy;
import de.mcpj.fabric.config.FabricConfigs;
import de.mcpj.fabric.entity.FabricPuddleJumperEntity;
import de.mcpj.fabric.entity.SegmentIndex;
import de.mcpj.fabric.gametest.FabricGameTestHooks;
import de.mcpj.fabric.network.FabricNetwork;
import de.mcpj.fabric.registry.FabricObjects;
import de.mcpj.mc.gametest.GameTestHooks;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.rules.JumperRules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;

/** MCPJ for Minecraft 1.20.1 on Fabric. */
public final class McpjFabric implements ModInitializer {
    private static final String TEAM_REBORN_ENERGY = "team_reborn_energy";

    @Override
    public void onInitialize() {
        McpjPlatform.set(new FabricPlatform());
        GameTestHooks.set(new FabricGameTestHooks());
        FabricConfigs.define();
        FabricObjects.register();
        FabricNetwork.registerServer();
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof FabricPuddleJumperEntity jumper) {
                SegmentIndex.add(jumper);
                jumper.loaded();
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof FabricPuddleJumperEntity jumper) SegmentIndex.remove(jumper);
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !JumperRules.isProtectedPassenger(entity, source));
        if (FabricLoader.getInstance().isModLoaded(TEAM_REBORN_ENERGY)) TeamRebornEnergy.register();
    }
}
