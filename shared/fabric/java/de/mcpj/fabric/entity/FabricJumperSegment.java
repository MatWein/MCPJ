package de.mcpj.fabric.entity;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.entity.jumper.JumperSegment;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A hull segment (bow or stern) on Fabric. It is not part of the level's entity list: like Forge's part entities it is
 * found by entity queries and id lookups through mixins (see SegmentIndex) and forwards everything to its jumper.
 */
public class FabricJumperSegment extends SegmentEntityCompat implements JumperSegment {
    private static final EntityDimensions SIZE = EntityDimensions.scalable(JumperGeometry.WIDTH, JumperGeometry.HEIGHT);
    private final FabricPuddleJumperEntity jumper;

    FabricJumperSegment(FabricPuddleJumperEntity jumper) {
        super(jumper.getType(), jumper.level());
        this.jumper = jumper;
        refreshDimensions();
    }

    @Override
    public FabricPuddleJumperEntity jumper() {
        return jumper;
    }

    @Override
    public boolean isPickable() {
        // the pilot sits inside the hull; otherwise the crosshair would always hit the own bow
        return !level().isClientSide() || !jumper.isVehicle();
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return jumper.getPickResult();
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || jumper == entity;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return other != jumper && !(other instanceof JumperSegment segment && segment.jumper() == jumper) && super.canCollideWith(other);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return SIZE;
    }

    /** Lightning hitting a segment charges the jumper (once per bolt), like a hit on the jumper itself. */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        jumper().lightning().struck(bolt);
    }
}
