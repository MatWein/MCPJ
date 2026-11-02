package de.mcpj.fabric.mixin;

import de.mcpj.mc.rules.JumperRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** A drone's explosion spares its own jumper and the people on board (Forge: ExplosionEvent.Detonate). */
@Mixin(Explosion.class)
abstract class ExplosionMixin {
    @Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> mcpj$spareFriends(Level level, Entity source, AABB box) {
        List<Entity> affected = level.getEntities(source, box);
        affected.removeIf(entity -> JumperRules.sparedByExplosion(source, entity));
        return affected;
    }
}
