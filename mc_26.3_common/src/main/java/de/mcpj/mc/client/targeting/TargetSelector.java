package de.mcpj.mc.client.targeting;

import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.Box;
import de.mcpj.core.targeting.TargetCycle;
import de.mcpj.core.targeting.TargetKind;
import de.mcpj.mc.util.Convert;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Tab selects the nearest target not yet chosen in the current round; after the last one no target is selected.
 * Chosen targets are remembered so moving mobs cannot reorder the cycle.
 */
/** The pilot's selected target (client side); the cycling rules are the core's {@link TargetCycle}. */
public final class TargetSelector {
    private static final TargetCycle CYCLE = new TargetCycle();
    @Nullable private static Entity target;

    private TargetSelector() {}

    @Nullable
    public static Entity target() {
        return target;
    }

    public static void clear() {
        target = null;
    }

    public static List<Entity> candidates(PuddleJumperEntity jumper) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        double range = ServerConfig.TARGETING.scanRange();
        Vec3 center = jumper.position();
        List<Entity> list = player.level().getEntities(jumper, jumper.getBoundingBox().inflate(range),
                entity -> entity != player && jumper.isValidTarget(entity) && distanceSqr(entity, center) <= range * range);
        list.sort(Comparator.comparingDouble(entity -> distanceSqr(entity, center)));
        return list;
    }

    /** Distance to the nearest point of the entity (for jumpers: of the whole hull). */
    public static double distanceSqr(Entity entity, Vec3 from) {
        Box box = entity instanceof PuddleJumperEntity jumper ? jumper.core().fullBox() : Convert.toCore(entity.getBoundingBox());
        return TargetCycle.distanceSqr(box, Convert.toCore(from));
    }

    public static TargetKind kind(Entity entity) {
        if (entity instanceof Enemy) return TargetKind.ENEMY;
        if (entity instanceof PuddleJumperEntity) return TargetKind.JUMPER;
        return TargetKind.NEUTRAL;
    }

    public static void cycle(PuddleJumperEntity jumper) {
        List<Entity> candidates = candidates(jumper);
        Integer next = CYCLE.next(jumper.level().getGameTime(), target != null ? target.getId() : null,
                candidates.stream().map(Entity::getId).toList());
        target = next == null ? null : candidates.stream().filter(entity -> entity.getId() == next).findFirst().orElse(null);
    }

    public static void validate(PuddleJumperEntity jumper) {
        if (target == null) return;
        double lockRange = ServerConfig.TARGETING.lockRange();
        if (!jumper.isValidTarget(target) || target.level() != jumper.level()
                || distanceSqr(target, jumper.position()) > lockRange * lockRange) {
            target = null;
        }
    }
}
