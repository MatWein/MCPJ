package de.mcpj.mc.entity.jumper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import de.mcpj.core.util.SaveData;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.util.NbtSaveData;
import de.mcpj.mc.util.SyncedDataDefiner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Minecraft 1.20.1's side of {@link PuddleJumperEntity}: the Entity methods whose signatures differ between
 * Minecraft versions. Every version has this class with the same hooks; the jumper itself is version-independent.
 */
public abstract class JumperEntityCompat extends Entity {
    private static final float MAX_STEP_HEIGHT = 1.0f;
    /** Cargo hold contents: a list of stacks with their slot (the format of MCPJ on 1.20.1 from the start). */
    private static final String CARGO_TAG = "Cargo";
    private static final String SLOT_TAG = "Slot";

    private final JumperInterpolation interpolation = new JumperInterpolation(this);

    protected JumperEntityCompat(EntityType<?> type, Level level) {
        super(type, level);
        setMaxUpStep(MAX_STEP_HEIGHT);
    }

    // ------------------------------------------------------------------ hooks (PuddleJumperEntity)

    protected abstract void defineJumperData(SyncedDataDefiner data);

    public abstract InteractionResult interactWith(Player player, InteractionHand hand);

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

    // ------------------------------------------------------------------ Minecraft 1.20.1

    @Override
    protected void defineSynchedData() {
        defineJumperData(new SyncedDataDefiner() {
            @Override
            public <T> void define(EntityDataAccessor<T> key, T value) {
                entityData.define(key, value);
            }
        });
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        interpolation.lerpTo(x, y, z, yRot);
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * PuddleJumperEntity.EYE_HEIGHT_FACTOR;
    }

    /** Seated players are placed by positionRider (a player's own riding offset is its seat drop). */
    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
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
        return interactWith(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && hurtJumper(source, amount);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    // dimension changes: each loader overrides its own changeDimension and calls changeDimensionWithPassengers

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        loadJumper(NbtSaveData.reading(tag));
        JumperCargo cargo = cargo();
        cargo.clearContent();
        ListTag list = tag.getList(CARGO_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte(SLOT_TAG) & 0xFF;
            if (slot < cargo.getContainerSize()) cargo.setItem(slot, ItemStack.of(entry));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        saveJumper(NbtSaveData.writing(tag));
        JumperCargo cargo = cargo();
        ListTag list = new ListTag();
        for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
            ItemStack stack = cargo.getItem(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte(SLOT_TAG, (byte) slot);
            stack.save(entry);
            list.add(entry);
        }
        tag.put(CARGO_TAG, list);
    }
}
