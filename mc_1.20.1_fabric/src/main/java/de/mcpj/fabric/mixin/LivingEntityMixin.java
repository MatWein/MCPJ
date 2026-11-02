package de.mcpj.fabric.mixin;

import de.mcpj.mc.rules.JumperRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Passengers of a cloaked jumper are invisible to mobs. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void mcpj$cloak(Entity looking, CallbackInfoReturnable<Double> result) {
        if (JumperRules.isCloakedPassenger((LivingEntity) (Object) this)) result.setReturnValue(0.0);
    }
}
