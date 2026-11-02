package de.mcpj.forge.event;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
        if (isProtected(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    /** /kill and the void still apply, otherwise a passenger could never die. */
    public static boolean isProtected(Entity entity, DamageSource source) {
        return entity.getVehicle() instanceof PuddleJumperEntity && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }
}
