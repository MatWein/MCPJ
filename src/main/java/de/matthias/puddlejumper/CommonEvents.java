package de.matthias.puddlejumper;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tarnung: Insassen eines getarnten Jumpers sind für Mobs unsichtbar. */
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
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (isCloakedPilot(event.getNewTarget())) event.setCanceled(true);
    }
}
