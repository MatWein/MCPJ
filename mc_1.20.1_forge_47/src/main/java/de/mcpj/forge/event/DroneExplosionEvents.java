package de.mcpj.forge.event;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.rules.JumperRules;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class DroneExplosionEvents {
    private DroneExplosionEvents() {}

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        Entity source = event.getExplosion().getDirectSourceEntity();
        event.getAffectedEntities().removeIf(affected -> JumperRules.sparedByExplosion(source, affected));
    }
}
