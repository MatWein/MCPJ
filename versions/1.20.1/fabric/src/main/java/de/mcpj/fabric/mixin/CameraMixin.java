package de.mcpj.fabric.mixin;

import de.mcpj.mc.client.camera.JumperCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The chase camera while piloting (Forge: ViewportEvent.ComputeCameraAngles). */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void mcpj$chaseCamera(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo callback) {
        JumperCamera.onCameraSetup((Camera) (Object) this, partialTick);
    }
}
