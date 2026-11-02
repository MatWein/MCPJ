package de.mcpj.mc.client.camera;

import de.mcpj.mc.client.PilotContext;
import de.mcpj.core.config.ClientConfig;
import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class JumperCamera {
    @Nullable private static CameraType cameraBeforeBoarding;

    private JumperCamera() {}

    /** End of every client tick: third person while piloting, the previous view afterwards. */
    public static void onClientTickEnd() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        boolean piloting = PilotContext.isPiloting();
        if (piloting && cameraBeforeBoarding == null) {
            cameraBeforeBoarding = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!piloting && cameraBeforeBoarding != null) {
            minecraft.options.setCameraType(cameraBeforeBoarding);
            cameraBeforeBoarding = null;
        }
    }

    /** After the camera was set up: the chase camera sits above the pilot and further back. */
    public static void onCameraSetup(Camera camera, float partialTick) {
        Entity viewer = camera.getEntity();
        if (!camera.isDetached() || viewer == null || !(viewer.getVehicle() instanceof PuddleJumperEntity)) return;
        Vec3 eye = viewer.getEyePosition(partialTick);
        camera.setPosition(eye.x, eye.y + JumperGeometry.CAMERA_HEIGHT, eye.z);
        camera.move(-camera.getMaxZoom(ClientConfig.cameraDistance()), 0, 0);
    }
}
