package de.mcpj.core.jumper;

import de.mcpj.core.config.LightConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft has no moving light sources, so the server places invisible light blocks
 * in the hull and ahead of the bow and removes them again behind the ship.
 */
public final class JumperLights {
    private static final int CENTER = 0;
    private static final int BOW = 1;
    private static final double STRANDED_DISTANCE_FACTOR = 2.0;
    private static final String LIGHT_BLOCKS_TAG = "LightBlocks";

    private final Jumper jumper;
    private final BlockPosition[] placed = new BlockPosition[2];
    private final List<BlockPosition> leftovers = new ArrayList<>();

    JumperLights(Jumper jumper) {
        this.jumper = jumper;
    }

    public List<BlockPosition> placedBlocks() {
        return Arrays.stream(placed).filter(Objects::nonNull).toList();
    }

    void tick() {
        leftovers.forEach(jumper.host()::removeLight);
        leftovers.clear();
        if (!jumper.isLightsOn() || jumper.isCloaked() || !jumper.host().isAlive()) {
            clear();
            return;
        }
        LightConfig config = ServerConfig.LIGHTS;
        boolean fast = jumper.serverVelocity().length() > config.singleLightSpeedPerTick();
        boolean moveTick = jumper.host().tickCount() % config.updateIntervalTicks() == 0;
        Vec3d center = jumper.host().location().add(0, JumperGeometry.CENTER_HEIGHT, 0);
        Vec3d[] targets = {center, center.add(jumper.forward().scale(JumperGeometry.BOW_LIGHT_DISTANCE))};
        for (int slot = 0; slot < placed.length; slot++) {
            if (slot == CENTER && fast) release(slot);
            else update(slot, targets[slot], moveTick, config.moveDistance());
        }
    }

    private void update(int slot, Vec3d target, boolean moveTick, double moveDistance) {
        BlockPosition current = placed[slot];
        if (current != null && (!moveTick || current.center().distanceTo(target) <= moveDistance)) return;
        BlockPosition next = BlockPosition.containing(target);
        if (next.equals(current) || next.equals(placed[otherSlot(slot)])) return;
        if (jumper.host().placeLight(next, ServerConfig.LIGHTS.lightLevel())) {
            if (current != null) jumper.host().removeLight(current);
            placed[slot] = next;
        } else if (current != null && current.center().distanceTo(target) > moveDistance * STRANDED_DISTANCE_FACTOR) {
            release(slot);
        }
    }

    private static int otherSlot(int slot) {
        return slot == CENTER ? BOW : CENTER;
    }

    private void release(int slot) {
        if (placed[slot] != null) jumper.host().removeLight(placed[slot]);
        placed[slot] = null;
    }

    void clear() {
        for (int slot = 0; slot < placed.length; slot++) release(slot);
    }

    public void load(SaveData data) {
        leftovers.clear();
        leftovers.addAll(data.getPositions(LIGHT_BLOCKS_TAG));
    }

    public void save(SaveData data) {
        List<BlockPosition> all = new ArrayList<>(leftovers);
        all.addAll(placedBlocks());
        data.putPositions(LIGHT_BLOCKS_TAG, all);
    }
}
