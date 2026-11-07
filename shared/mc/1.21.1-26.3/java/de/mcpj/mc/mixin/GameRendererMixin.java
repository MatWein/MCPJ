package de.mcpj.mc.mixin;

import de.mcpj.mc.client.render.PilotVisibility;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No first person hand while piloting. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void mcpj$hideHand(CallbackInfo callback) {
        if (PilotVisibility.hidesHand()) callback.cancel();
    }
}
