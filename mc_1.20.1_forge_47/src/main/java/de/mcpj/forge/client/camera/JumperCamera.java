package de.mcpj.forge.client.camera;

import de.mcpj.forge.McpjMod;
import de.mcpj.forge.client.PilotContext;
import de.mcpj.core.config.ClientConfig;
import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.forge.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID, value = Dist.CLIENT)
public final class JumperCamera {
    @Nullable private static CameraType cameraBeforeBoarding;

    private JumperCamera() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.player == null) return;
        boolean piloting = PilotContext.isPiloting();
        if (piloting && cameraBeforeBoarding == null) {
            cameraBeforeBoarding = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!piloting && cameraBeforeBoarding != null) {
            minecraft.options.setCameraType(cameraBeforeBoarding);
            cameraBeforeBoarding = null;
        }
    }

    @SubscribeEvent
    public static void onComputeCamera(ViewportEvent.ComputeCameraAngles event) {
        Camera camera = event.getCamera();
        Entity viewer = camera.getEntity();
        if (!camera.isDetached() || viewer == null || !(viewer.getVehicle() instanceof PuddleJumperEntity)) return;
        Vec3 eye = viewer.getEyePosition((float) event.getPartialTick());
        camera.setPosition(eye.x, eye.y + JumperGeometry.CAMERA_HEIGHT, eye.z);
        camera.move(-camera.getMaxZoom(ClientConfig.cameraDistance()), 0, 0);
    }
}
