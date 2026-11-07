package de.mcpj.neoforge.entity;

import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.entity.PartEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Minecraft 1.21.1's side of the jumper's hull parts: the Entity methods whose signatures differ between
 * Minecraft versions (all of them hand over to the jumper). The parts itself is version-independent.
 */
public abstract class PartEntityCompat extends PartEntity<PuddleJumperEntity> {
    protected PartEntityCompat(PuddleJumperEntity jumper) {
        super(jumper);
    }

    public abstract PuddleJumperEntity jumper();

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && jumper().hurt(source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return jumper().interact(player, hand);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /** Hull parts never travel on their own; the jumper takes care of that. */
    @Override
    @Nullable
    public Entity changeDimension(DimensionTransition transition) {
        return null;
    }

    /** Whatever destroys a part destroys the whole ship. */
    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()) jumper().kill();
    }
}
