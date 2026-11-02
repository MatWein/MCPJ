package de.matthias.puddlejumper.client;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import de.matthias.puddlejumper.network.JumperActionPacket;
import de.matthias.puddlejumper.network.ModNetwork;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;

@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    /** Kameraabstand in der Third-Person-Ansicht (Vanilla: 4). */
    private static final double CAMERA_DISTANCE = 11.0 * PuddleJumperEntity.RENDER_SIZE;

    /** Ansicht vor dem Einsteigen; null = nicht im Jumper. */
    private static CameraType cameraBeforeBoarding;

    private ClientForgeEvents() {}

    /** Beim Einsteigen in die Außenansicht wechseln, beim Aussteigen die alte Ansicht wiederherstellen. */
    private static void updateCamera(boolean inJumper) {
        Minecraft mc = Minecraft.getInstance();
        if (inJumper && cameraBeforeBoarding == null) {
            cameraBeforeBoarding = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!inJumper && cameraBeforeBoarding != null) {
            mc.options.setCameraType(cameraBeforeBoarding);
            cameraBeforeBoarding = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Im Jumper liegen Q/E auf Seitwärtsflug: Wegwerfen und Inventar unterdrücken (vor Vanilla-Auswertung)
            if (ClientState.pilotedJumper() != null) {
                Minecraft mc = Minecraft.getInstance();
                if (KeyBindings.STRAFE_LEFT.getKey().equals(mc.options.keyDrop.getKey())) while (mc.options.keyDrop.consumeClick()) {}
                if (KeyBindings.STRAFE_RIGHT.getKey().equals(mc.options.keyInventory.getKey())) while (mc.options.keyInventory.consumeClick()) {}
                if (KeyBindings.LIGHTS.getKey().equals(mc.options.keyAdvancements.getKey())) while (mc.options.keyAdvancements.consumeClick()) {}
            }
            return;
        }
        PuddleJumperEntity jumper = ClientState.pilotedJumper();
        if (Minecraft.getInstance().player != null) updateCamera(jumper != null);
        while (KeyBindings.TARGET.consumeClick()) {} // wird über InputEvent.Key behandelt
        while (KeyBindings.CARGO.consumeClick()) {}
        while (KeyBindings.STRAFE_LEFT.consumeClick()) {}
        while (KeyBindings.STRAFE_RIGHT.consumeClick()) {}
        if (jumper == null) {
            ClientState.target = null;
            // Tastendrücke außerhalb des Jumpers verwerfen
            while (KeyBindings.CLOAK.consumeClick()) {}
            while (KeyBindings.SHIELD.consumeClick()) {}
            while (KeyBindings.PODS.consumeClick()) {}
            while (KeyBindings.LIGHTS.consumeClick()) {}
            return;
        }
        while (KeyBindings.CLOAK.consumeClick()) send(JumperActionPacket.Action.CLOAK, -1);
        while (KeyBindings.SHIELD.consumeClick()) send(JumperActionPacket.Action.SHIELD, -1);
        while (KeyBindings.PODS.consumeClick()) send(JumperActionPacket.Action.PODS, -1);
        while (KeyBindings.LIGHTS.consumeClick()) send(JumperActionPacket.Action.LIGHTS, -1);
        ClientState.validateTarget(jumper);
        // Energie unter 10 %: leiser Warnton alle 2 s
        if (jumper.getEnergyFraction() < 0.1f && jumper.tickCount % 40 == 0) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.get(), 0.6f, 0.35f));
        }
    }

    /** Tab im Jumper: Ziel durchschalten (eigener Tasten-Kontext, außerhalb bleibt Tab die Spielerliste). */
    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS || Minecraft.getInstance().screen != null) return;
        PuddleJumperEntity jumper = ClientState.pilotedJumper();
        if (jumper != null && KeyBindings.CARGO.getKey().getValue() == event.getKey()) {
            Minecraft.getInstance().player.sendOpenInventory(); // Server öffnet den Laderaum (HasCustomInventoryScreen)
            return;
        }
        if (jumper != null && KeyBindings.TARGET.getKey().getValue() == event.getKey()) {
            ClientState.cycleTarget(jumper);
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), ClientState.target != null ? 1.6f : 1.0f, 0.25f));
        }
    }

    /** Im Jumper liegt Tab auf der Zielerfassung - Spielerliste dort ausblenden. */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_LIST.id()) && ClientState.pilotedJumper() != null) {
            event.setCanceled(true);
        }
    }

    /** Linksklick im Jumper = Drohne abfeuern statt Angriff. */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        PuddleJumperEntity jumper = ClientState.pilotedJumper();
        if (jumper == null) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        long now = jumper.level().getGameTime();
        if (now - ClientState.lastFireTick >= PuddleJumperEntity.DRONE_COOLDOWN) {
            ClientState.lastFireTick = now;
            send(JumperActionPacket.Action.FIRE, ClientState.target != null ? ClientState.target.getId() : -1);
        }
    }

    private static void send(JumperActionPacket.Action action, int target) {
        ModNetwork.sendToServer(new JumperActionPacket(action, target));
    }

    /** Pilot sitzt im Rumpf: Spielermodell ausblenden. */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity().getVehicle() instanceof PuddleJumperEntity) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientState.pilotedJumper() != null) event.setCanceled(true);
    }

    /** Größerer Kameraabstand in der Third-Person-Ansicht, damit der ganze Jumper ins Bild passt. */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Camera cam = event.getCamera();
        if (!cam.isDetached()) return;
        Entity e = cam.getEntity();
        if (e == null || !(e.getVehicle() instanceof PuddleJumperEntity)) return;
        float pt = (float) event.getPartialTick();
        Vec3 eye = e.getEyePosition(pt);
        cam.setPosition(eye.x, eye.y + 1.2 * PuddleJumperEntity.RENDER_SIZE, eye.z);
        cam.move(-cam.getMaxZoom(CAMERA_DISTANCE), 0, 0);
    }
}
