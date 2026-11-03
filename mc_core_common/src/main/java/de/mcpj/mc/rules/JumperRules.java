package de.mcpj.mc.rules;

import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** Rules each loader hooks into the game (Forge with events, Fabric with callbacks and mixins). */
public final class JumperRules {
    /** Like a player's invulnerability after a hit: at most one redirected hit per half second and passenger. */
    private static final int PASSENGER_HIT_COOLDOWN_TICKS = 20;

    private JumperRules() {}

    /**
     * Damage to an entity, as the loaders report it: seated passengers are not hurt (see {@link #isProtectedPassenger}),
     * an attack on them hits the hull instead, so mobs going for the pilot attack the ship.
     *
     * @return true if the damage to the entity itself is cancelled
     */
    public static boolean shieldPassenger(Entity entity, DamageSource source, float amount) {
        if (!isProtectedPassenger(entity, source)) return false;
        if (entity.getVehicle() instanceof PuddleJumperEntity jumper && isAttackOnBoard(jumper, source)
                && McCompat.invulnerableTime(entity) <= PASSENGER_HIT_COOLDOWN_TICKS / 2) {
            McCompat.setInvulnerableTime(entity, PASSENGER_HIT_COOLDOWN_TICKS);
            McCompat.hurt(jumper, source, amount);
        }
        return true;
    }

    /** An attack from outside; explosions are excluded because they hit the hull themselves. */
    private static boolean isAttackOnBoard(PuddleJumperEntity jumper, DamageSource source) {
        Entity attacker = source.getEntity();
        return attacker != null && attacker != jumper && !jumper.hasPassenger(attacker) && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

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
