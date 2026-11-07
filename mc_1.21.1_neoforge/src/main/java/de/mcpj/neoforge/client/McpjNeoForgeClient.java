package de.mcpj.neoforge.client;

import de.mcpj.mc.block.JumperProjectorBlock;
import de.mcpj.mc.client.projector.ProjectorStartDialog;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import de.mcpj.mc.client.render.DeferredBubbles;
import de.mcpj.mc.client.ClientCompat;
import de.mcpj.core.jumper.FlightInput;
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
import de.mcpj.neoforge.entity.NeoForgePuddleJumperEntity;
import de.mcpj.neoforge.registry.NeoForgeObjects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * MCPJ's client on NeoForge: renderers, keys, HUD and ticks for the shared client code. Camera, keyboard, attack and
 * hiding the pilot are vanilla mixins shared with Fabric (mcpj.mixins.json).
 */
@Mod(value = Mcpj.MOD_ID, dist = Dist.CLIENT)
public final class McpjNeoForgeClient {
    public McpjNeoForgeClient(IEventBus modBus) {
        FlightInput.source = FlightInputReader::read;
        PuddleJumperEntity.clientTicker = EngineSounds::tick;
        JumperProjectorBlock.startDialog = ProjectorStartDialog::open;
        PilotSoundPacket.player = PilotSounds::play;

        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerEntityRenderer(NeoForgeObjects.PUDDLE_JUMPER.get(), PuddleJumperRenderer<NeoForgePuddleJumperEntity>::new);
            event.registerEntityRenderer(NeoForgeObjects.DRONE.get(), DroneRenderer::new);
        });
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> KeyBindings.ALL.forEach(event::register));
        modBus.addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(ModelCache.INSTANCE));
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.HOTBAR, Mcpj.id("cockpit"),
                (graphics, deltaTracker) -> {
                    JumperHud.render(ClientCompat.hudCanvas(graphics), graphics.guiWidth(), graphics.guiHeight());
                    TargetMarkers.render(graphics);
                }));

        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(ClientTickEvent.Pre.class, event -> PilotInputHandler.onClientTickStart());
        gameBus.addListener(ClientTickEvent.Post.class, event -> {
            PilotInputHandler.onClientTickEnd();
            JumperCamera.onClientTickEnd();
            LowEnergyAlarm.onClientTickEnd();
        });
        gameBus.addListener(RenderLevelStageEvent.class, McpjNeoForgeClient::onRenderStage);
    }

    /**
     * Shield bubbles are drawn after the weather (see DeferredBubbles), target markers projected after the translucent
     * blocks. Since Minecraft 1.21 the camera rotation is not on the pose stack: the entities' poses lack it and
     * RenderSystem's model-view matrix still holds it at AFTER_WEATHER.
     */
    private static void onRenderStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
            DeferredBubbles.clear();
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            TargetMarkers.collect(event.getModelViewMatrix(), event.getProjectionMatrix(), event.getCamera(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            DeferredBubbles.renderPending(null);
        }
    }
}
