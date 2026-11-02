package de.mcpj.forge.client;

import de.mcpj.forge.McpjMod;
import de.mcpj.forge.client.hud.JumperHud;
import de.mcpj.forge.client.input.FlightInputReader;
import de.mcpj.forge.client.input.KeyBindings;
import de.mcpj.forge.client.render.DroneRenderer;
import de.mcpj.forge.client.render.ModelCache;
import de.mcpj.forge.client.render.PuddleJumperRenderer;
import de.mcpj.forge.client.sound.EngineSounds;
import de.mcpj.forge.client.sound.PilotSounds;
import de.mcpj.forge.client.targeting.TargetMarkers;
import de.mcpj.core.jumper.FlightInput;
import de.mcpj.forge.entity.jumper.PuddleJumperEntity;
import de.mcpj.forge.network.PilotSoundPacket;
import de.mcpj.forge.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        FlightInput.source = FlightInputReader::read;
        PuddleJumperEntity.clientTicker = EngineSounds::tick;
        PilotSoundPacket.player = PilotSounds::play;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PUDDLE_JUMPER.get(), PuddleJumperRenderer::new);
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.ALL.forEach(event::register);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ModelCache.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "jumper_hud", JumperHud::render);
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "jumper_targets", TargetMarkers::render);
    }
}
