package de.mcpj.fabric.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * Jumpers per level, so entity queries (hits, collisions, crosshair) and id lookups (interaction packets) also find
 * their hull segments. Updated from Fabric's entity load/unload events; read by mixins on Level and ServerLevel.
 */
public final class SegmentIndex {
    private static final Map<Level, Set<FabricPuddleJumperEntity>> JUMPERS = new WeakHashMap<>();

    private SegmentIndex() {}

    public static synchronized void add(FabricPuddleJumperEntity jumper) {
        JUMPERS.computeIfAbsent(jumper.level(), level -> Collections.newSetFromMap(new WeakHashMap<>())).add(jumper);
    }

    public static synchronized void remove(FabricPuddleJumperEntity jumper) {
        Set<FabricPuddleJumperEntity> jumpers = JUMPERS.get(jumper.level());
        if (jumpers != null) jumpers.remove(jumper);
    }

    /** Adds the segments in {@code box} that pass {@code filter} (except {@code except}) to {@code out}. */
    public static synchronized void collect(Level level, @Nullable Entity except, AABB box, Predicate<? super Entity> filter, List<Entity> out) {
        Set<FabricPuddleJumperEntity> jumpers = JUMPERS.get(level);
        if (jumpers == null) return;
        for (FabricPuddleJumperEntity jumper : jumpers) {
            for (FabricJumperSegment segment : jumper.segments()) {
                if (segment != except && segment.getBoundingBox().intersects(box) && filter.test(segment)) out.add(segment);
            }
        }
    }

    @Nullable
    public static synchronized Entity segment(Level level, int id) {
        Set<FabricPuddleJumperEntity> jumpers = JUMPERS.get(level);
        if (jumpers == null) return null;
        for (FabricPuddleJumperEntity jumper : jumpers) {
            for (FabricJumperSegment segment : jumper.segments()) {
                if (segment.getId() == id) return segment;
            }
        }
        return null;
    }
}
