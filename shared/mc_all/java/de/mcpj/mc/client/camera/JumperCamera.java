package de.mcpj.mc.client.camera;

import de.mcpj.mc.client.ClientCompat;
import de.mcpj.core.config.ClientConfig;
import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class JumperCamera {
    @Nullable private static CameraType cameraBeforeBoarding;

    private JumperCamera() {}

    /** End of every client tick: third person while aboard (pilot or passenger), the previous view afterwards. */
    public static void onClientTickEnd() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        // the hull has no modelled cabin: passengers get the same chase camera as the pilot
        boolean aboard = minecraft.player.getVehicle() instanceof PuddleJumperEntity;
        if (aboard && cameraBeforeBoarding == null) {
            cameraBeforeBoarding = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!aboard && cameraBeforeBoarding != null) {
            minecraft.options.setCameraType(cameraBeforeBoarding);
            cameraBeforeBoarding = null;
        }
    }

    /** After the camera was set up: the chase camera sits above the pilot and further back. */
    public static void onCameraSetup(Camera camera, float partialTick) {
        Entity viewer = ClientCompat.cameraEntity(camera);
        if (!camera.isDetached() || viewer == null || !(viewer.getVehicle() instanceof PuddleJumperEntity)) return;
        Vec3 eye = viewer.getEyePosition(partialTick);
        camera.setPosition(eye.x, eye.y + JumperGeometry.CAMERA_HEIGHT, eye.z);
        camera.move(-camera.getMaxZoom((float) ClientConfig.cameraDistance()), 0, 0);
    }
}
