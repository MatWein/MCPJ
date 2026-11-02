package de.mcpj.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.mcpj.mc.client.render.PilotVisibility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No first person hand while piloting (Forge: RenderHandEvent). */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void mcpj$hideHand(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers, LocalPlayer player,
                               int light, CallbackInfo callback) {
        if (PilotVisibility.hidesHand()) callback.cancel();
    }
}
