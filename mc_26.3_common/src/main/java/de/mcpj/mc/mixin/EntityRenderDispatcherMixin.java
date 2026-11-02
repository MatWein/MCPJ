package de.mcpj.mc.mixin;

import de.mcpj.mc.client.render.PilotVisibility;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Passengers of a jumper sit inside the hull and are not drawn. */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void mcpj$hidePassengers(E entity, Frustum culler, double camX, double camY, double camZ, float partialTicks,
                                                       CallbackInfoReturnable<Boolean> result) {
        if (entity instanceof LivingEntity && PilotVisibility.hidesEntity(entity)) result.setReturnValue(false);
    }
}
