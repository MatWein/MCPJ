package de.matthias.puddlejumper.client;

import de.matthias.puddlejumper.ModRegistry;
import de.matthias.puddlejumper.entity.FlightInput;
import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        FlightInput.source = ClientModEvents::readInput;
        PuddleJumperEntity.clientTicker = ClientState::tickJumper;
    }

    private static FlightInput readInput() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null) return FlightInput.NONE;
        float up = (mc.options.keyJump.isDown() ? 1f : 0f) - (mc.options.keySprint.isDown() ? 1f : 0f);
        // A/D (leftImpulse) dreht das Schiff, die Maus steuert nur die Kamera
        return new FlightInput(p.input.forwardImpulse, p.input.leftImpulse, up);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.PUDDLE_JUMPER.get(), PuddleJumperRenderer::new);
        event.registerEntityRenderer(ModRegistry.DRONE.get(), DroneRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.CLOAK);
        event.register(KeyBindings.PODS);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ModelCache.INSTANCE);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "jumper_hud", JumperHud::render);
    }
}
