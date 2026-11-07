package de.mcpj.forge.event;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.rules.JumperRules;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Whoever sits in a jumper cannot be hurt: the hull takes the hits instead. When the jumper is destroyed its
 * passengers are thrown out first and are vulnerable again (see JumperHull#destroy).
 */
@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class PassengerEvents {
    private PassengerEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (JumperRules.shieldPassenger(event.getEntity(), event.getSource(), event.getAmount())) event.setCanceled(true);
    }
}
