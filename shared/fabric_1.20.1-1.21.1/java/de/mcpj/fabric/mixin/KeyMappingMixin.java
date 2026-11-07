package de.mcpj.fabric.mixin;

import de.mcpj.fabric.client.SharedKeyMapping;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * Keys that may share a key with vanilla (Tab, Q, E, I, L) stay out of vanilla's key-to-binding map, so the vanilla
 * binding keeps that key; MCPJ reads them as raw keys while piloting (Forge solves this with conflict contexts).
 */
@Mixin(KeyMapping.class)
abstract class KeyMappingMixin {
    @Shadow
    @Final
    private static Map<?, KeyMapping> MAP;

    @Redirect(method = {"<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILjava/lang/String;)V", "resetMapping"},
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object mcpj$keepVanillaKeys(Map map, Object key, Object binding) {
        if (map == MAP && binding instanceof SharedKeyMapping) return null;
        return map.put(key, binding);
    }
}
