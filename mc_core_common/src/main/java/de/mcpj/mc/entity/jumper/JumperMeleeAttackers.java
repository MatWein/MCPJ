package de.mcpj.mc.entity.jumper;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.compat.McCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Melee mobs that go for someone on board attack the hull: their reach is measured to the hull instead of the pilot
 * sitting inside (out of reach). The hit itself goes to the pilot and from there to the hull (JumperRules).
 * Ranged attackers and creepers need no help: arrows and explosions hit the hull anyway.
 */
final class JumperMeleeAttackers {
    /** Like MeleeAttackGoal: one hit per second. */
    private static final int ATTACK_INTERVAL_TICKS = 20;
    private static final double SEARCH_RANGE = 4.0;

    private final PuddleJumperEntity jumper;
    private final Map<Mob, Integer> nextAttack = new WeakHashMap<>();

    JumperMeleeAttackers(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    /** Server, every tick. */
    void tick() {
        if (!jumper.isVehicle()) {
            nextAttack.clear();
            return;
        }
        AABB area = jumper.getBoundingBox().inflate(JumperGeometry.SEGMENT_OFFSET + SEARCH_RANGE);
        for (Mob mob : jumper.level().getEntitiesOfClass(Mob.class, area, this::isMeleeAttackerOfPassenger)) {
            if (jumper.tickCount < nextAttack.getOrDefault(mob, 0) || !withinReach(mob)) continue;
            nextAttack.put(mob, jumper.tickCount + ATTACK_INTERVAL_TICKS);
            McCompat.meleeAttack(mob, mob.getTarget());
        }
    }

    private boolean isMeleeAttackerOfPassenger(Mob mob) {
        LivingEntity target = mob.getTarget();
        return target != null && target.getVehicle() == jumper && mob.isAlive()
                && !(mob instanceof RangedAttackMob) && !(mob instanceof Creeper) && mob.getAttribute(Attributes.ATTACK_DAMAGE) != null;
    }

    /** Vanilla's melee reach is about the mob's width beyond its own body. */
    private boolean withinReach(Mob mob) {
        AABB reach = mob.getBoundingBox().inflate(mob.getBbWidth());
        if (reach.intersects(jumper.getBoundingBox())) return true;
        for (Entity segment : jumper.segments()) {
            if (reach.intersects(segment.getBoundingBox())) return true;
        }
        return false;
    }
}
