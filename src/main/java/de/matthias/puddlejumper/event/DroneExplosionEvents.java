package de.matthias.puddlejumper.event;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.entity.drone.DroneEntity;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PuddleJumperMod.MOD_ID)
public final class DroneExplosionEvents {
    private DroneExplosionEvents() {}

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getDirectSourceEntity() instanceof DroneEntity drone) {
            event.getAffectedEntities().removeIf(drone::isFriendly);
        }
    }
}
