package de.mcpj.client.targeting;

import de.mcpj.core.config.ServerConfig;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tab selects the nearest target not yet chosen in the current round; after the last one no target is selected.
 * Chosen targets are remembered so moving mobs cannot reorder the cycle.
 */
public final class TargetSelector {
    private static final int CYCLE_RESET_TICKS = 60;
    private static final long NEVER = -1_000_000L;
    private static final Set<Integer> VISITED = new HashSet<>();
    @Nullable private static Entity target;
    private static long lastCycleTick = NEVER;

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

    public static double distanceSqr(Entity entity, Vec3 from) {
        AABB box = entity instanceof PuddleJumperEntity jumper ? jumper.fullBoundingBox() : entity.getBoundingBox();
        double dx = Math.max(0, Math.max(box.minX - from.x, from.x - box.maxX));
        double dy = Math.max(0, Math.max(box.minY - from.y, from.y - box.maxY));
        double dz = Math.max(0, Math.max(box.minZ - from.z, from.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    public static void cycle(PuddleJumperEntity jumper) {
        long now = jumper.level().getGameTime();
        if (now - lastCycleTick > CYCLE_RESET_TICKS) VISITED.clear();
        lastCycleTick = now;
        if (target != null) VISITED.add(target.getId());
        target = candidates(jumper).stream().filter(entity -> !VISITED.contains(entity.getId())).findFirst().orElse(null);
        if (target == null) VISITED.clear();
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
