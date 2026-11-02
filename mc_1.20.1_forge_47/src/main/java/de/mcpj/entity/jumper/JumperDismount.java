package de.mcpj.entity.jumper;

import de.mcpj.core.jumper.JumperGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class JumperDismount {
    private JumperDismount() {}

    @Nullable
    static Vec3 find(PuddleJumperEntity jumper, LivingEntity passenger) {
        Vec3 forward = jumper.forward();
        Vec3 left = jumper.left();
        Vec3 origin = jumper.position();
        Vec3[] candidates = {
                origin.add(forward.scale(-JumperGeometry.DISMOUNT_LONGITUDINAL)),
                origin.add(left.scale(JumperGeometry.DISMOUNT_LATERAL)),
                origin.add(left.scale(-JumperGeometry.DISMOUNT_LATERAL)),
                origin.add(forward.scale(JumperGeometry.DISMOUNT_LONGITUDINAL))};
        for (Vec3 candidate : candidates) {
            Vec3 standing = standingSpot(jumper, passenger, BlockPos.containing(candidate));
            if (standing != null) return standing;
        }
        Vec3 midAir = origin.add(left.scale(JumperGeometry.DISMOUNT_LATERAL));
        boolean free = jumper.level().noCollision(passenger, passenger.getBoundingBox().move(midAir.subtract(passenger.position())));
        return free ? midAir : null;
    }

    @Nullable
    private static Vec3 standingSpot(PuddleJumperEntity jumper, LivingEntity passenger, BlockPos base) {
        for (BlockPos pos : new BlockPos[]{base, base.above(), base.below()}) {
            double floor = jumper.level().getBlockFloorHeight(pos);
            if (!DismountHelper.isBlockFloorValid(floor)) continue;
            Vec3 spot = Vec3.upFromBottomCenterOf(pos, floor);
            for (Pose pose : passenger.getDismountPoses()) {
                if (DismountHelper.canDismountTo(jumper.level(), passenger, passenger.getLocalBoundsForPose(pose).move(spot))) {
                    passenger.setPose(pose);
                    return spot;
                }
            }
        }
        return null;
    }
}
