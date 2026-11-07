package de.mcpj.neoforge.entity;

import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Minecraft 26.3's side of the jumper's hull parts: the Entity methods whose signatures differ between
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
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return !isInvulnerableToBase(source) && jumper().hurtServer(level, source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return jumper().interact(player, hand, location);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    /** Hull parts never travel on their own; the jumper takes care of that. */
    @Override
    @Nullable
    public Entity teleport(TeleportTransition transition) {
        return null;
    }

    /** Whatever destroys a part destroys the whole ship. */
    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel level) jumper().kill(level);
    }
}
