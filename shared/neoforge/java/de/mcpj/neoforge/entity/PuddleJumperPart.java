package de.mcpj.neoforge.entity;

import de.mcpj.mc.entity.jumper.JumperSegment;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** A hull segment (bow or stern) as a NeoForge part entity; everything goes to its jumper. */
public class PuddleJumperPart extends PartEntityCompat implements JumperSegment {
    private final EntityDimensions size;
    private final PuddleJumperEntity jumper;

    PuddleJumperPart(PuddleJumperEntity jumper, float width, float height) {
        super(jumper);
        this.jumper = jumper;
        this.size = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }

    @Override
    public PuddleJumperEntity jumper() {
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
        return size;
    }

    /** Lightning hitting a segment charges the jumper (once per bolt), like a hit on the jumper itself. */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        jumper().lightning().struck(bolt);
    }
}
