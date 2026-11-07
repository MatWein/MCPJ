package de.mcpj.mc.entity.jumper;

import net.minecraft.world.ContainerHelper;
import de.mcpj.core.util.SaveData;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.util.NbtSaveData;
import de.mcpj.mc.util.SyncedDataDefiner;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Minecraft 26.3's side of {@link PuddleJumperEntity}: the Entity methods whose signatures differ between
 * Minecraft versions. Every version has this class with the same hooks; the jumper itself is version-independent.
 */
public abstract class JumperEntityCompat extends Entity {
    private static final float MAX_STEP_HEIGHT = 1.0f;
    /** Cargo hold contents, as a container in the jumper's data. */
    private static final String CARGO_TAG = "Cargo";
    private static final int INTERPOLATION_STEPS = 3;

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
        return isLocalInstanceAuthoritative();
    }

    public boolean entityDropsEnabled() {
        return level() instanceof ServerLevel level && level.getGameRules().get(GameRules.ENTITY_DROPS);
    }

    public void dropShipItem() {
        if (level() instanceof ServerLevel level) spawnAtLocation(level, McpjPlatform.get().jumperItem());
    }

    protected InteractionResult interactionSuccess() {
        return InteractionResult.SUCCESS;
    }

    /** Right after the entity tick: network interpolation (Minecraft 26.3 does it itself, see below). */
    protected void interpolationTick() {}

    // ------------------------------------------------------------------ Minecraft 26.3

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
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, INTERPOLATION_STEPS);
    }

    @Override
    public float maxUpStep() {
        return MAX_STEP_HEIGHT;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return interactWith(player);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return !isInvulnerableToBase(source) && hurtJumper(source, amount);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Nullable
    @Override
    public Entity teleport(TeleportTransition transition) {
        return changeDimensionWithPassengers(() -> super.teleport(transition));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        loadJumper(NbtSaveData.reading(input));
        cargo().clearContent();
        input.child(CARGO_TAG).ifPresent(items -> ContainerHelper.loadAllItems(items, cargo().getItems()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        saveJumper(NbtSaveData.writing(output));
        ContainerHelper.saveAllItems(output.child(CARGO_TAG), cargo().getItems());
    }
}
