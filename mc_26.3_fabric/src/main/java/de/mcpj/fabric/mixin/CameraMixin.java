package de.mcpj.fabric.mixin;

import de.mcpj.mc.client.camera.JumperCamera;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The chase camera while piloting: placed after vanilla's third person setup, before the view frustum. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void mcpj$chaseCamera(float partialTick, CallbackInfo callback) {
        JumperCamera.onCameraSetup((Camera) (Object) this, partialTick);
    }
}
