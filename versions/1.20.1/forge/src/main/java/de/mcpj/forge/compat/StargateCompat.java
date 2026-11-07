package de.mcpj.forge.compat;

import com.mojang.logging.LogUtils;
import de.mcpj.mc.entity.jumper.JumperTravel;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Just Stargate Mod: its event horizon sends every entity in range on its own, including seated passengers and the
 * jumper's hull segments, also within the same dimension (no Forge travel event there). This cancels JSG's
 * {@code StargateSendTravelerEvent} for them, so only the jumper travels and takes its passengers along.
 *
 * <p>JSG only closes a gate automatically after a player went through it, and it counts only players it sends itself.
 * Players on board a jumper are therefore reported to the sending gate when the jumper is sent.
 *
 * <p>JSG is optional and not on a public maven, so its API is accessed by reflection; if it changes, the
 * integration logs a warning and stays inactive (the mod works the same without JSG).
 */
public final class StargateCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "jsg";
    private static final String SEND_EVENT = "dev.tauri.jsg.api.event.StargateSendTravelerEvent";
    private static final String TRAVELER = "dev.tauri.jsg.api.stargate.traveler.IStargateTraveler";
    private static final String STARGATE = "dev.tauri.jsg.api.stargate.Stargate";
    // not part of JSG's API: the only way to count a player that JSG did not send itself
    private static final String AUTO_CLOSE_MANAGER = "dev.tauri.jsg.stargate.manager.StargateAutoCloseManager";

    private StargateCompat() {}

    public static void register() {
        if (!ModList.get().isLoaded(MOD_ID)) return;
        Class<? extends Event> eventType;
        Method getTraveler;
        Method getEntity;
        try {
            eventType = Class.forName(SEND_EVENT).asSubclass(Event.class);
            getTraveler = eventType.getMethod("getTraveler");
            getEntity = Class.forName(TRAVELER).getMethod("get");
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            LOGGER.warn("Just Stargate Mod found, but its API changed; passengers may be separated from jumpers in stargates", e);
            return;
        }
        listenForSeparateTravelers(eventType, getTraveler, getEntity);
        LOGGER.info("Just Stargate Mod found: jumpers travel through stargates with their passengers");
        try {
            AutoClose autoClose = new AutoClose(Class.forName(TRAVELER).getMethod("getTransmitter"),
                    Class.forName(STARGATE).getMethod("getAutoCloseManager"),
                    Class.forName(AUTO_CLOSE_MANAGER).getMethod("playerPassing"));
            listenForPilots(eventType, getTraveler, getEntity, autoClose);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Just Stargate Mod changed; stargates may stay open after a jumper with players went through", e);
        }
    }

    /** Seated passengers and hull segments are not sent on their own: the jumper takes them along. */
    private static <T extends Event> void listenForSeparateTravelers(Class<T> eventType, Method getTraveler, Method getEntity) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, eventType, event -> {
            if (travelerEntity(event, getTraveler, getEntity) instanceof Entity entity && JumperTravel.mustNotTravelAlone(entity)) {
                event.setCanceled(true);
            }
        });
    }

    /** A jumper is sent (not cancelled by anyone): count its players as passing, like players walking through. */
    private static <T extends Event> void listenForPilots(Class<T> eventType, Method getTraveler, Method getEntity, AutoClose autoClose) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, eventType, event -> {
            if (!(travelerEntity(event, getTraveler, getEntity) instanceof PuddleJumperEntity jumper)) return;
            for (Entity passenger : jumper.getPassengers()) {
                if (passenger instanceof Player) autoClose.playerPassing(invoke(getTraveler, event));
            }
        });
    }

    private record AutoClose(Method getTransmitter, Method getAutoCloseManager, Method playerPassing) {
        void playerPassing(@Nullable Object traveler) {
            Object manager = invoke(getAutoCloseManager, invoke(getTransmitter, traveler));
            if (playerPassing.getDeclaringClass().isInstance(manager)) invoke(playerPassing, manager);
        }
    }

    @Nullable
    private static Object travelerEntity(Event event, Method getTraveler, Method getEntity) {
        return invoke(getEntity, invoke(getTraveler, event));
    }

    @Nullable
    private static Object invoke(Method method, @Nullable Object target) {
        if (target == null) return null;
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
