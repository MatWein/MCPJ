package de.mcpj.forge.event;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.rules.JumperRules;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class CloakEvents {
    private CloakEvents() {}

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (JumperRules.isCloakedPassenger(event.getEntity())) event.modifyVisibility(0.0);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (JumperRules.isCloakedPassenger(event.getNewTarget())) event.setCanceled(true);
    }
}
