package de.mcpj.neoforge.event;

import de.mcpj.mc.entity.jumper.JumperTravel;
import de.mcpj.mc.rules.JumperRules;
import de.mcpj.neoforge.registry.NeoForgeObjects;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** NeoForge's events for the shared rules: protected passengers, travel together, the pad's energy capability. */
public final class ServerEvents {
    private ServerEvents() {}

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(ServerEvents::onRegisterCapabilities);
        // whoever sits in a jumper cannot be hurt: the hull takes the hits instead (see JumperHull#destroy)
        gameBus.addListener(EventPriority.HIGH, LivingIncomingDamageEvent.class, event -> {
            if (JumperRules.isProtectedPassenger(event.getEntity(), event.getSource())) event.setCanceled(true);
        });
        // explosions spare what the rules protect (e.g. the own jumper from its drone's blast)
        gameBus.addListener(ExplosionEvent.Detonate.class, event -> {
            Entity source = event.getExplosion().getDirectSourceEntity();
            event.getAffectedEntities().removeIf(affected -> JumperRules.sparedByExplosion(source, affected));
        });
        // seated passengers and hull segments never change dimension on their own; the jumper takes them along
        gameBus.addListener(EventPriority.HIGH, EntityTravelToDimensionEvent.class, event -> {
            if (JumperTravel.mustNotTravelAlone(event.getEntity())) event.setCanceled(true);
        });
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, NeoForgeObjects.CHARGING_PAD_ENTITY.get(), (pad, side) -> pad.energy());
    }
}
