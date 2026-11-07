package de.mcpj.mc.mixin;

import de.mcpj.mc.client.input.PilotInputHandler;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raw key presses for keys that may share a key with vanilla (Forge: InputEvent.Key). */
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void mcpj$key(long window, int key, int scanCode, int action, int modifiers, CallbackInfo callback) {
        if (window == Minecraft.getInstance().getWindow().getWindow()) PilotInputHandler.onKey(key, action);
    }
}
