package de.mcpj.forge.entity.jumper;

import de.mcpj.mc.entity.jumper.JumperSegment;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

/** A hull segment (bow or stern) as a Forge part entity. */
public class PuddleJumperPart extends PartEntity<PuddleJumperEntity> implements JumperSegment {
    private final EntityDimensions size;

    public PuddleJumperPart(PuddleJumperEntity parent, float width, float height) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }

    @Override
    public PuddleJumperEntity jumper() {
        return getParent();
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() {
        // The pilot sits inside the hull; otherwise the crosshair would always hit the own bow.
        return !level().isClientSide || !getParent().isVehicle();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurt(source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return getParent().interact(player, hand);
    }

    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return other != getParent() && !(other instanceof JumperSegment segment && segment.jumper() == getParent())
                && super.canCollideWith(other);
    }

    /** Hull segments never travel on their own (see JumperTravel); the jumper takes care of that. */
    @Override
    @Nullable
    public Entity changeDimension(ServerLevel destination, ITeleporter teleporter) {
        return null;
    }

    /** Whatever destroys a segment (e.g. a stargate kawoosh) destroys the whole ship. */
    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()) getParent().kill();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }
}
