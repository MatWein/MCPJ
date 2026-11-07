package de.mcpj.fabric.mixin;

import de.mcpj.mc.rules.JumperRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mobs cannot target passengers of a cloaked jumper. */
@Mixin(Mob.class)
abstract class MobMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void mcpj$cloak(LivingEntity target, CallbackInfo callback) {
        if (JumperRules.isCloakedPassenger(target)) callback.cancel();
    }
}
