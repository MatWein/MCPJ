package de.mcpj.mc.util;

import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Box;
import de.mcpj.core.math.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Conversions between the core's value types and Minecraft's. */
public final class Convert {
    private Convert() {}

    public static Vec3 toMc(Vec3d v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    public static Vec3d toCore(Vec3 v) {
        return new Vec3d(v.x, v.y, v.z);
    }

    public static AABB toMc(Box box) {
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    public static Box toCore(AABB box) {
        return new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public static BlockPos toMc(BlockPosition pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    public static BlockPosition toCore(BlockPos pos) {
        return new BlockPosition(pos.getX(), pos.getY(), pos.getZ());
    }
}
