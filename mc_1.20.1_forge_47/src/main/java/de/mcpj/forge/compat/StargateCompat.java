package de.mcpj.forge.compat;

import com.mojang.logging.LogUtils;
import de.mcpj.mc.entity.jumper.JumperTravel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Just Stargate Mod: its event horizon sends every entity in range on its own, including seated passengers and the
 * jumper's hull segments, also within the same dimension (no Forge travel event there). This cancels JSG's
 * {@code StargateSendTravelerEvent} for them, so only the jumper travels and takes its passengers along.
 *
 * <p>JSG is optional and not on a public maven, so its API is accessed by reflection; if it changes, the
 * integration logs a warning and stays inactive.
 */
public final class StargateCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "jsg";
    private static final String SEND_EVENT = "dev.tauri.jsg.api.event.StargateSendTravelerEvent";
    private static final String TRAVELER = "dev.tauri.jsg.api.stargate.traveler.IStargateTraveler";

    private StargateCompat() {}

    public static void register() {
        if (!ModList.get().isLoaded(MOD_ID)) return;
        try {
            Class<? extends Event> eventType = Class.forName(SEND_EVENT).asSubclass(Event.class);
            listen(eventType, eventType.getMethod("getTraveler"), Class.forName(TRAVELER).getMethod("get"));
            LOGGER.info("Just Stargate Mod found: jumpers travel through stargates with their passengers");
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOGGER.warn("Just Stargate Mod found, but its API changed; passengers may be separated from jumpers in stargates", e);
        }
    }

    private static <T extends Event> void listen(Class<T> eventType, Method getTraveler, Method getEntity) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, eventType, event -> {
            if (travelerEntity(event, getTraveler, getEntity) instanceof Entity entity && JumperTravel.mustNotTravelAlone(entity)) {
                event.setCanceled(true);
            }
        });
    }

    private static Object travelerEntity(Event event, Method getTraveler, Method getEntity) {
        try {
            return getEntity.invoke(getTraveler.invoke(event));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
