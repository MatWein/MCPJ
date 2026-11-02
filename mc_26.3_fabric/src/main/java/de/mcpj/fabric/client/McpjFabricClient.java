package de.mcpj.fabric.client;

import de.mcpj.core.jumper.FlightInput;
import de.mcpj.fabric.config.FabricConfigs;
import de.mcpj.fabric.entity.FabricPuddleJumperEntity;
import de.mcpj.fabric.entity.SegmentIndex;
import de.mcpj.fabric.network.FabricNetwork;
import de.mcpj.fabric.registry.FabricObjects;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.client.camera.JumperCamera;
import de.mcpj.mc.client.hud.JumperHud;
import de.mcpj.mc.client.input.FlightInputReader;
import de.mcpj.mc.client.input.KeyBindings;
import de.mcpj.mc.client.input.PilotInputHandler;
import de.mcpj.mc.client.render.DroneRenderer;
import de.mcpj.mc.client.render.ModelCache;
import de.mcpj.mc.client.render.PuddleJumperRenderer;
import de.mcpj.mc.client.sound.EngineSounds;
import de.mcpj.mc.client.sound.LowEnergyAlarm;
import de.mcpj.mc.client.sound.PilotSounds;
import de.mcpj.mc.client.targeting.TargetMarkers;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.network.PilotSoundPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

/** MCPJ's client on Fabric: renderers, keys, HUD and Fabric's client events for the shared client code. */
public final class McpjFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricConfigs.CLIENT.load();
        FlightInput.source = FlightInputReader::read;
        PuddleJumperEntity.clientTicker = EngineSounds::tick;
        PilotSoundPacket.player = PilotSounds::play;

        EntityRendererRegistry.register(FabricObjects.PUDDLE_JUMPER, PuddleJumperRenderer<FabricPuddleJumperEntity>::new);
        EntityRendererRegistry.register(FabricObjects.DRONE, DroneRenderer::new);
        KeyBindings.ALL.forEach(KeyMappingHelper::registerKeyMapping);
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Mcpj.id("models"), ModelCache.INSTANCE);

        HudElementRegistry.addLast(Mcpj.id("cockpit"), (graphics, deltaTracker) -> {
            JumperHud.render(graphics, graphics.guiWidth(), graphics.guiHeight());
            TargetMarkers.render(graphics);
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> PilotInputHandler.onClientTickStart());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PilotInputHandler.onClientTickEnd();
            JumperCamera.onClientTickEnd();
            LowEnergyAlarm.onClientTickEnd();
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof FabricPuddleJumperEntity jumper) SegmentIndex.add(jumper);
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof FabricPuddleJumperEntity jumper) SegmentIndex.remove(jumper);
        });

        // handlers run on the client thread
        ClientPlayNetworking.registerGlobalReceiver(PilotSoundPacket.TYPE, (packet, context) -> packet.handle());
        ClientPlayNetworking.registerGlobalReceiver(FabricNetwork.ServerConfigPacket.TYPE,
                (packet, context) -> FabricConfigs.SERVER.apply(packet.values()));
        // back to the own settings after leaving a server
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(FabricConfigs.SERVER::load));
    }
}
