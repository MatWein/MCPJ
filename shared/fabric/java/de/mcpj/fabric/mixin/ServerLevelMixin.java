package de.mcpj.fabric.mixin;

import de.mcpj.fabric.entity.SegmentIndex;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Interaction and attack packets name an entity by id; that may be a jumper hull segment. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Inject(method = "getEntityOrPart", at = @At("RETURN"), cancellable = true)
    private void mcpj$findSegment(int id, CallbackInfoReturnable<Entity> result) {
        if (result.getReturnValue() == null) {
            Entity segment = SegmentIndex.segment((ServerLevel) (Object) this, id);
            if (segment != null) result.setReturnValue(segment);
        }
    }
}
