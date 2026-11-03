package de.mcpj.neoforge.entity;

import de.mcpj.mc.entity.jumper.JumperSegment;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

/** A hull segment (bow or stern) as a NeoForge part entity; everything goes to its jumper. */
public class PuddleJumperPart extends PartEntity<PuddleJumperEntity> implements JumperSegment {
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
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean isPickable() {
        // the pilot sits inside the hull; otherwise the crosshair would always hit the own bow
        return !level().isClientSide() || !jumper.isVehicle();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return !isInvulnerableToBase(source) && jumper.hurtServer(level, source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return jumper.interact(player, hand, location);
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
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
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

    /** Hull segments never travel on their own; the jumper takes care of that. */
    @Override
    @Nullable
    public Entity teleport(TeleportTransition transition) {
        return null;
    }

    /** Whatever destroys a segment destroys the whole ship. */
    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel level) jumper.kill(level);
    }

    /** Lightning hitting a segment charges the jumper (once per bolt), like a hit on the jumper itself. */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        jumper().lightning().struck(bolt);
    }
}
