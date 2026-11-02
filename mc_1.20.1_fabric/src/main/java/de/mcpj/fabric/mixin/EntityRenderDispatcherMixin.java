package de.mcpj.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.mcpj.mc.client.render.PilotVisibility;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Whoever sits in a jumper is not drawn (Forge: RenderLivingEvent.Pre). */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void mcpj$hidePassengers(E entity, double x, double y, double z, float yaw, float partialTick,
                                                       PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (entity instanceof LivingEntity && PilotVisibility.hidesEntity(entity)) callback.cancel();
    }
}
