package de.mcpj.mc.mixin;

import de.mcpj.mc.client.input.PilotInputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The attack key launches drones while piloting (Forge: InputEvent.InteractionKeyMappingTriggered). */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void mcpj$fire(CallbackInfoReturnable<Boolean> result) {
        if (PilotInputHandler.onAttack()) result.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void mcpj$keepFiring(boolean leftClick, CallbackInfo callback) {
        if (leftClick && PilotInputHandler.onAttack()) callback.cancel();
    }
}
