package de.mcpj.neoforge.event;

import de.mcpj.neoforge.block.InputEnergyStorage;
import de.mcpj.mc.entity.jumper.JumperTravel;
import de.mcpj.mc.rules.JumperRules;
import de.mcpj.neoforge.registry.NeoForgeObjects;
import de.mcpj.mc.entity.jumper.JumperLightning;
import net.minecraft.world.entity.LightningBolt;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
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
            if (JumperRules.shieldPassenger(event.getEntity(), event.getSource(), event.getAmount())) event.setCanceled(true);
        });
        // explosions spare what the rules protect (e.g. the own jumper from its drone's blast)
        gameBus.addListener(ExplosionEvent.Detonate.class, event -> {
            Entity source = event.getExplosion().getDirectSourceEntity();
            event.getAffectedEntities().removeIf(affected -> JumperRules.sparedByExplosion(source, affected));
        });
        // jumpers attract natural lightning during thunderstorms
        gameBus.addListener(EntityJoinLevelEvent.class, event -> {
            if (event.getEntity() instanceof LightningBolt bolt) JumperLightning.attract(bolt);
        });
        // seated passengers and hull segments never change dimension on their own; the jumper takes them along
        gameBus.addListener(EventPriority.HIGH, EntityTravelToDimensionEvent.class, event -> {
            if (JumperTravel.mustNotTravelAlone(event.getEntity())) event.setCanceled(true);
        });
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, NeoForgeObjects.CHARGING_PAD_ENTITY.get(), (pad, side) -> pad.energy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, NeoForgeObjects.JUMPER_PROJECTOR_ENTITY.get(),
                (projector, side) -> new InputEnergyStorage(projector.logic()));
    }
}
