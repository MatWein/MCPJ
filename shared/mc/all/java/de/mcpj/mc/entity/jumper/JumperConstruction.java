package de.mcpj.mc.entity.jumper;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Vec3d;
import de.mcpj.core.util.SaveData;
import de.mcpj.mc.block.JumperProjectorBlockEntity;
import de.mcpj.mc.util.Convert;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A jumper while a projector builds it: shown like the pilot sees a cloaked ship (glowing outline, brighter as the
 * build goes on) with scanning sparks; frozen, invulnerable and closed. Removes itself if its projector is gone.
 * Hidden (no outline, no sparks) while the projector lacks energy for the build.
 */
public final class JumperConstruction {
    /** Synced progress value of a finished (normal) jumper. */
    public static final float DONE = -1f;
    private static final int PROJECTOR_CHECK_INTERVAL = 20;
    private static final int SCAN_TICKS = 40;
    private static final int SPARKS_PER_TICK = 3;
    private static final double BEAM_SPEED = 0.08;
    private static final String PROGRESS_TAG = "Construction";
    private static final String PROJECTOR_TAG = "Projector";

    private final PuddleJumperEntity jumper;
    @Nullable private BlockPos projector;

    JumperConstruction(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    public boolean isActive() {
        return jumper.constructionProgress() >= 0;
    }

    /** Build progress 0..1 (0 if not under construction). */
    public float progress() {
        return Math.max(0f, jumper.constructionProgress());
    }

    /** Server: the projector at {@code projector} starts building this jumper (before it enters the world). */
    public void begin(BlockPos projector) {
        this.projector = projector;
        jumper.setShielded(false);
        jumper.setCloaked(false);
        jumper.setLightsOn(false);
        jumper.setConstructionProgress(0f);
    }

    /** Under construction but not shown: the projector has no energy for the build. */
    public boolean isHidden() {
        return isActive() && jumper.constructionHidden();
    }

    public void update(float progress, boolean powered) {
        jumper.setConstructionProgress(Math.max(0f, Math.min(1f, progress)));
        jumper.setConstructionHidden(!powered);
    }

    public void finish() {
        projector = null;
        jumper.setConstructionHidden(false);
        jumper.setConstructionProgress(DONE);
    }

    /** Every tick while under construction (instead of the normal ship logic). */
    void tick() {
        Level level = jumper.level();
        if (!level.isClientSide()) {
            if (jumper.tickCount % PROJECTOR_CHECK_INTERVAL == 0 && !hasProjector(level)) jumper.discard();
            return;
        }
        if (!isHidden()) spawnEffects(level);
    }

    private boolean hasProjector(Level level) {
        if (projector == null) return false;
        if (!level.isLoaded(projector)) return true;
        return level.getBlockEntity(projector) instanceof JumperProjectorBlockEntity entity && entity.isBuilding(jumper);
    }

    /** Sparks on a plane scanning the hull from bottom to top, and a beam rising from the projector. */
    private void spawnEffects(Level level) {
        RandomSource random = jumper.getRandom();
        float scan = (jumper.tickCount % SCAN_TICKS) / (float) SCAN_TICKS;
        double y = jumper.getY() + scan * JumperGeometry.HEIGHT;
        Vec3d forward = jumper.core().forward();
        Vec3d left = jumper.core().left();
        double halfLength = JumperGeometry.SEGMENT_OFFSET + JumperGeometry.WIDTH / 2.0;
        for (int i = 0; i < SPARKS_PER_TICK; i++) {
            Vec3d at = Convert.toCore(jumper.position())
                    .add(forward.scale((random.nextDouble() * 2 - 1) * halfLength))
                    .add(left.scale((random.nextDouble() - 0.5) * JumperGeometry.WIDTH));
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x(), y, at.z(), 0, 0, 0);
        }
        if (jumper.tickCount % 2 == 0) {
            level.addParticle(ParticleTypes.END_ROD, jumper.getX() + (random.nextDouble() - 0.5) * 0.3, jumper.getY(),
                    jumper.getZ() + (random.nextDouble() - 0.5) * 0.3, 0, BEAM_SPEED, 0);
        }
    }

    void load(SaveData data) {
        jumper.setConstructionProgress(data.contains(PROGRESS_TAG) ? data.getFloat(PROGRESS_TAG) : DONE);
        List<BlockPosition> positions = data.getPositions(PROJECTOR_TAG);
        projector = positions.isEmpty() ? null : Convert.toMc(positions.get(0));
    }

    void save(SaveData data) {
        if (!isActive()) return;
        data.putFloat(PROGRESS_TAG, jumper.constructionProgress());
        data.putPositions(PROJECTOR_TAG, projector == null ? List.of() : List.of(Convert.toCore(projector)));
    }
}
