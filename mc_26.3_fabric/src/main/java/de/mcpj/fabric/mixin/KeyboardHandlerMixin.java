package de.mcpj.fabric.mixin;

import de.mcpj.mc.client.input.PilotInputHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** All jumper keys are read as raw key presses (see PilotInputHandler). */
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void mcpj$key(long handle, int action, KeyEvent event, CallbackInfo callback) {
        if (handle == Minecraft.getInstance().getWindow().handle()) PilotInputHandler.onKey(event.key(), action);
    }
}
