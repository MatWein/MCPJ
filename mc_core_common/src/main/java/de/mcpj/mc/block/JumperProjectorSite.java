package de.mcpj.mc.block;

import de.mcpj.core.jumper.Jumper;
import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.core.math.Box;
import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.util.Convert;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Where a projector builds its jumper: standing on top of the block, nose in the block's facing. */
public final class JumperProjectorSite {
    private JumperProjectorSite() {}

    private static Vec3 jumperPosition(BlockPos projector) {
        return new Vec3(projector.getX() + 0.5, projector.getY() + 1, projector.getZ() + 0.5);
    }

    /** The three hull segments of the jumper to build. */
    static List<AABB> hull(BlockPos projector, Direction facing) {
        Vec3 center = jumperPosition(projector);
        Vec3 forward = new Vec3(facing.getStepX(), 0, facing.getStepZ());
        List<AABB> boxes = new ArrayList<>();
        for (double offset : Jumper.segmentOffsets()) {
            Vec3 segment = center.add(forward.scale(offset));
            boxes.add(Convert.toMc(Box.around(Convert.toCore(segment), JumperGeometry.WIDTH / 2.0, JumperGeometry.HEIGHT)));
        }
        return boxes;
    }

    /** No blocks and no other ship where the jumper stands ({@code own}: the jumper already being built). */
    public static boolean isSpaceFree(Level level, BlockPos projector, Direction facing, @Nullable Entity own) {
        for (AABB box : hull(projector, facing)) {
            if (level.getBlockCollisions(null, box).iterator().hasNext()) return false;
            if (!level.getEntitiesOfClass(PuddleJumperEntity.class, box, jumper -> jumper != own).isEmpty()) return false;
        }
        return true;
    }

    /** Places a jumper under construction above the projector; null if that failed. */
    @Nullable
    public static PuddleJumperEntity spawnJumper(ServerLevel level, BlockPos projector, Direction facing) {
        Vec3 position = jumperPosition(projector);
        PuddleJumperEntity jumper = McCompat.newJumper(level, position.x, position.y, position.z, facing.toYRot());
        if (jumper == null) return null;
        jumper.construction().begin(projector);
        return level.addFreshEntity(jumper) ? jumper : null;
    }
}
