package de.matthias.puddlejumper.client;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import de.matthias.puddlejumper.network.JumperActionPacket;
import de.matthias.puddlejumper.network.ModNetwork;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
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
    private static final double CAMERA_DISTANCE = 11.0;

    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PuddleJumperEntity jumper = ClientState.pilotedJumper();
        if (jumper == null) {
            ClientState.target = null;
            // Tastendrücke außerhalb des Jumpers verwerfen
            while (KeyBindings.CLOAK.consumeClick()) {}
            while (KeyBindings.HATCH.consumeClick()) {}
            while (KeyBindings.PODS.consumeClick()) {}
            return;
        }
        while (KeyBindings.CLOAK.consumeClick()) send(JumperActionPacket.Action.CLOAK, -1);
        while (KeyBindings.HATCH.consumeClick()) send(JumperActionPacket.Action.HATCH, -1);
        while (KeyBindings.PODS.consumeClick()) send(JumperActionPacket.Action.PODS, -1);
        ClientState.updateTarget(jumper);
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
        cam.setPosition(eye.x, eye.y + 1.2, eye.z);
        cam.move(-cam.getMaxZoom(CAMERA_DISTANCE), 0, 0);
    }
}
