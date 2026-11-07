package de.mcpj.fabric.mixin;

import de.mcpj.mc.rules.JumperRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Explosions spare what the rules protect (e.g. the own jumper from its drone's blast). */
@Mixin(ServerExplosion.class)
abstract class ExplosionMixin {
    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> mcpj$spareFriends(ServerLevel level, Entity source, AABB box) {
        List<Entity> affected = level.getEntities(source, box);
        affected.removeIf(entity -> JumperRules.sparedByExplosion(source, entity));
        return affected;
    }
}
