package de.mcpj.mc.rules;

import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** Rules each loader hooks into the game (Forge with events, Fabric with callbacks and mixins). */
public final class JumperRules {
    private JumperRules() {}

    /** Passengers cannot be hurt while seated; /kill and the void still apply, otherwise they could never die. */
    public static boolean isProtectedPassenger(Entity entity, DamageSource source) {
        return entity.getVehicle() instanceof PuddleJumperEntity && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** Mobs neither see nor target passengers of a cloaked jumper. */
    public static boolean isCloakedPassenger(@Nullable Entity entity) {
        return entity != null && entity.getVehicle() instanceof PuddleJumperEntity jumper && jumper.isCloaked();
    }

    /** A drone's explosion spares its own jumper, pilot and fellow passengers. */
    public static boolean sparedByExplosion(@Nullable Entity explosionSource, Entity affected) {
        return explosionSource instanceof DroneEntity drone && drone.isFriendly(affected);
    }
}
