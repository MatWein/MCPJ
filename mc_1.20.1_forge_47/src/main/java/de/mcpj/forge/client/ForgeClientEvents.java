package de.mcpj.forge.client;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.client.camera.JumperCamera;
import de.mcpj.mc.client.input.PilotInputHandler;
import de.mcpj.mc.client.render.DeferredBubbles;
import de.mcpj.mc.client.render.PilotVisibility;
import de.mcpj.mc.client.sound.LowEnergyAlarm;
import de.mcpj.mc.client.targeting.TargetMarkers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge's client events, handed to the shared client code. */
@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID, value = Dist.CLIENT)
public final class ForgeClientEvents {
    private ForgeClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PilotInputHandler.onClientTickStart();
            return;
        }
        PilotInputHandler.onClientTickEnd();
        JumperCamera.onClientTickEnd();
        LowEnergyAlarm.onClientTickEnd();
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        PilotInputHandler.onKey(event.getKey(), event.getAction());
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && PilotInputHandler.onAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onComputeCamera(ViewportEvent.ComputeCameraAngles event) {
        JumperCamera.onCameraSetup(event.getCamera(), (float) event.getPartialTick());
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
            DeferredBubbles.clear();
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            TargetMarkers.collect(event.getPoseStack(), event.getProjectionMatrix(), event.getCamera(), event.getPartialTick());
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            DeferredBubbles.renderPending(event.getPoseStack().last());
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (PilotVisibility.hidesEntity(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (PilotVisibility.hidesHand()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_LIST.id()) && PilotVisibility.hidesPlayerList()) {
            event.setCanceled(true);
        }
    }
}
