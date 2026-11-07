package de.mcpj.fabric.mixin;

import de.mcpj.mc.client.render.PilotVisibility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tab cycles targets while piloting instead of showing the player list. */
@Mixin(PlayerTabOverlay.class)
abstract class PlayerTabOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void mcpj$hide(GuiGraphics graphics, int width, Scoreboard scoreboard, Objective objective, CallbackInfo callback) {
        if (PilotVisibility.hidesPlayerList()) callback.cancel();
    }
}
