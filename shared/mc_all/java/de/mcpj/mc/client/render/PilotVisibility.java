package de.mcpj.mc.client.render;

import de.mcpj.mc.client.PilotContext;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.world.entity.Entity;

/** While piloting, the pilot is hidden inside the hull, without a first person hand, and Tab cycles targets. */
public final class PilotVisibility {
    private PilotVisibility() {}

    /** Living entities sitting in a jumper are not drawn. */
    public static boolean hidesEntity(Entity entity) {
        return entity.getVehicle() instanceof PuddleJumperEntity;
    }

    public static boolean hidesHand() {
        return PilotContext.isPiloting();
    }

    /** Tab selects targets while piloting instead of showing the player list. */
    public static boolean hidesPlayerList() {
        return PilotContext.isPiloting();
    }
}
