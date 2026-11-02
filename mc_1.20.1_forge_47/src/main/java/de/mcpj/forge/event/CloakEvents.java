package de.mcpj.forge.event;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class CloakEvents {
    private CloakEvents() {}

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (isCloakedPassenger(event.getEntity())) event.modifyVisibility(0.0);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (isCloakedPassenger(event.getNewTarget())) event.setCanceled(true);
    }

    private static boolean isCloakedPassenger(Entity entity) {
        return entity != null && entity.getVehicle() instanceof PuddleJumperEntity jumper && jumper.isCloaked();
    }
}
