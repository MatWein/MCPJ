package de.mcpj.forge.event;

import de.mcpj.forge.McpjMod;
import de.mcpj.forge.entity.drone.DroneEntity;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class DroneExplosionEvents {
    private DroneExplosionEvents() {}

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getDirectSourceEntity() instanceof DroneEntity drone) {
            event.getAffectedEntities().removeIf(drone::isFriendly);
        }
    }
}
