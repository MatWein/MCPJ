package de.mcpj.fabric.client;

import de.mcpj.mc.client.ClientCompat;
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
import de.mcpj.mc.client.render.DeferredBubbles;
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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
        KeyBindings.ALL.forEach(KeyBindingHelper::registerKeyBinding);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ModelReloader());

        HudRenderCallback.EVENT.register((graphics, partialTick) -> {
            JumperHud.render(ClientCompat.hudCanvas(graphics), graphics.guiWidth(), graphics.guiHeight());
            TargetMarkers.render(graphics);
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> PilotInputHandler.onClientTickStart());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PilotInputHandler.onClientTickEnd();
            JumperCamera.onClientTickEnd();
            LowEnergyAlarm.onClientTickEnd();
        });
        WorldRenderEvents.START.register(context -> DeferredBubbles.clear());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> TargetMarkers.collect(context.positionMatrix(),
                context.projectionMatrix(), context.camera(), context.tickCounter().getGameTimeDeltaPartialTick(false)));
        // LAST comes after clouds and weather, when the camera rotation is already off the model-view stack
        WorldRenderEvents.LAST.register(context -> DeferredBubbles.renderPending(null));

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

    /** Reloads the OBJ models with the resource packs. */
    private static final class ModelReloader implements IdentifiableResourceReloadListener {
        private static final ResourceLocation ID = Mcpj.id("models");

        @Override
        public ResourceLocation getFabricId() {
            return ID;
        }

        @Override
        public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager resources,
                                              ProfilerFiller preparationProfiler, ProfilerFiller reloadProfiler,
                                              Executor background, Executor game) {
            return ModelCache.INSTANCE.reload(barrier, resources, preparationProfiler, reloadProfiler, background, game);
        }
    }
}
