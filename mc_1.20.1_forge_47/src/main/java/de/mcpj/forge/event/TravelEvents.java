package de.mcpj.forge.event;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.entity.jumper.JumperTravel;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Seated passengers and hull segments never change dimension on their own; the jumper takes them along. */
@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class TravelEvents {
    private TravelEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (JumperTravel.mustNotTravelAlone(event.getEntity())) event.setCanceled(true);
    }
}
