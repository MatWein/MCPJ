package de.mcpj.mc.entity.jumper;

import net.minecraft.world.ContainerHelper;
import de.mcpj.core.util.SaveData;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.util.NbtSaveData;
import de.mcpj.mc.util.SyncedDataDefiner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Minecraft 1.21.1's side of {@link PuddleJumperEntity}: the Entity methods whose signatures differ between
 * Minecraft versions. Every version has this class with the same hooks; the jumper itself is version-independent.
 */
public abstract class JumperEntityCompat extends Entity {
    private static final float MAX_STEP_HEIGHT = 1.0f;
    /** Cargo hold contents, as a container in the jumper's data. */
    private static final String CARGO_TAG = "Cargo";

    private final JumperInterpolation interpolation = new JumperInterpolation(this);

    protected JumperEntityCompat(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ------------------------------------------------------------------ hooks (PuddleJumperEntity)

    protected abstract void defineJumperData(SyncedDataDefiner data);

    protected abstract InteractionResult interactWith(Player player);

    protected abstract boolean hurtJumper(DamageSource source, float amount);

    @Nullable
    protected abstract Entity changeDimensionWithPassengers(Supplier<@Nullable Entity> change);

    protected abstract void loadJumper(SaveData data);

    protected abstract void saveJumper(SaveData data);

    public abstract JumperCargo cargo();

    public abstract AABB cullingBox();

    // ------------------------------------------------------------------ for the version-independent code

    /** Whether this game instance moves the ship (the pilot's client, or the server without pilot). */
    public boolean isControlledLocally() {
        return isControlledByLocalInstance();
    }

    public boolean entityDropsEnabled() {
        return level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS);
    }

    public void dropShipItem() {
        spawnAtLocation(McpjPlatform.get().jumperItem());
    }

    protected InteractionResult interactionSuccess() {
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    /** Right after the entity tick: network interpolation on the other clients. */
    protected void interpolationTick() {
        interpolation.tick();
    }

    // ------------------------------------------------------------------ Minecraft 1.21.1

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        defineJumperData(new SyncedDataDefiner() {
            @Override
            public <T> void define(EntityDataAccessor<T> key, T value) {
                builder.define(key, value);
            }
        });
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        interpolation.lerpTo(x, y, z, yRot);
    }

    @Override
    public float maxUpStep() {
        return MAX_STEP_HEIGHT;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return cullingBox();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return interactWith(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && hurtJumper(source, amount);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition transition) {
        return changeDimensionWithPassengers(() -> super.changeDimension(transition));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        loadJumper(NbtSaveData.reading(tag));
        cargo().clearContent();
        ContainerHelper.loadAllItems(tag.getCompound(CARGO_TAG), cargo().getItems(), registryAccess());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        saveJumper(NbtSaveData.writing(tag));
        CompoundTag items = new CompoundTag();
        ContainerHelper.saveAllItems(items, cargo().getItems(), registryAccess());
        tag.put(CARGO_TAG, items);
    }
}
