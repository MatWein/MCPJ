package de.mcpj.fabric.mixin;

import de.mcpj.fabric.entity.SegmentIndex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/** Entity queries also return jumper hull segments (hits, collisions, crosshair), like Forge's part entities. */
@Mixin(Level.class)
abstract class LevelMixin {
    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"))
    private void mcpj$addSegments(Entity except, AABB box, Predicate<? super Entity> filter, CallbackInfoReturnable<List<Entity>> result) {
        SegmentIndex.collect((Level) (Object) this, except, box, filter, result.getReturnValue());
    }
}
