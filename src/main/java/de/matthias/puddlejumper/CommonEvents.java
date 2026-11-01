package de.matthias.puddlejumper;

import de.matthias.puddlejumper.entity.DroneEntity;
import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tarnung (Insassen für Mobs unsichtbar) und Drohnen-Explosionen (verschonen den eigenen Jumper). */
@Mod.EventBusSubscriber(modid = PuddleJumperMod.MODID)
public final class CommonEvents {
    private CommonEvents() {}

    private static boolean isCloakedPilot(Entity e) {
        return e != null && e.getVehicle() instanceof PuddleJumperEntity j && j.isCloaked();
    }

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (isCloakedPilot(event.getEntity())) event.modifyVisibility(0.0);
    }

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getDirectSourceEntity() instanceof DroneEntity drone) {
            event.getAffectedEntities().removeIf(drone::isFriendly);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (isCloakedPilot(event.getNewTarget())) event.setCanceled(true);
    }
}
