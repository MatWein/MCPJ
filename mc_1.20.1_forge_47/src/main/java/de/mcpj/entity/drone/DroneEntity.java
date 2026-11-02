package de.mcpj.entity.drone;

import de.mcpj.core.config.DroneConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import de.mcpj.entity.jumper.PuddleJumperPart;
import de.mcpj.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DroneEntity extends Projectile {
    public static final float SIZE = 0.5f;
    private static final double KNOCKBACK = 0.6;
    private static final double KNOCKBACK_LIFT = 0.25;
    private static final float ROTATION_SMOOTHING = 0.5f;
    private static final double RENDER_DISTANCE = 128;
    private static final String LIFE_TAG = "Life";
    private static final String JUMPER_TAG = "Jumper";

    private final DroneGuidance guidance = new DroneGuidance(this);
    private int age;
    @Nullable private UUID jumperId;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public DroneEntity(Level level, LivingEntity owner, PuddleJumperEntity jumper, @Nullable Entity target) {
        this(ModEntities.DRONE.get(), level);
        setOwner(owner);
        jumperId = jumper.getUUID();
        guidance.setTarget(target);
    }

    public RandomSource getRandom() {
        return random;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            DroneEffects.trail(this);
        } else if (!serverTick()) {
            return;
        }
        Vec3 velocity = getDeltaMovement();
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        ProjectileUtil.rotateTowardsMovement(this, ROTATION_SMOOTHING);
    }

    private boolean serverTick() {
        if (++age > ServerConfig.DRONES.lifetimeTicks()) {
            DroneEffects.fizzle(this);
            discard();
            return false;
        }
        setDeltaMovement(guidance.steer(age));
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            onHit(hit);
        }
        return !isRemoved();
    }

    @Override
    public boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof DroneEntity) && !isFriendly(entity);
    }

    /** The launching jumper (including its hull segments), its pilot and fellow passengers. */
    public boolean isFriendly(Entity entity) {
        if (entity instanceof PuddleJumperEntity jumper && isOwnJumper(jumper)) return true;
        if (entity instanceof PuddleJumperPart part && isOwnJumper(part.getParent())) return true;
        if (entity.getVehicle() instanceof PuddleJumperEntity jumper && isOwnJumper(jumper)) return true;
        Entity owner = getOwner();
        return owner != null && (entity == owner || entity.isPassengerOfSameVehicle(owner));
    }

    private boolean isOwnJumper(PuddleJumperEntity jumper) {
        return jumper.getUUID().equals(jumperId);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        DroneConfig config = ServerConfig.DRONES;
        hit.hurt(damageSources().thrown(this, getOwner()), config.damage());
        Vec3 push = getDeltaMovement().normalize().scale(KNOCKBACK);
        hit.push(push.x, KNOCKBACK_LIFT, push.z);
        explode(result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explode(result.getLocation());
    }

    private void explode(Vec3 at) {
        if (level() instanceof ServerLevel level) {
            level.explode(this, at.x, at.y, at.z, ServerConfig.DRONES.explosionPower(), Level.ExplosionInteraction.MOB);
            DroneEffects.explosion(level, at);
        }
        discard();
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < RENDER_DISTANCE * RENDER_DISTANCE;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(LIFE_TAG, age);
        if (jumperId != null) tag.putUUID(JUMPER_TAG, jumperId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = tag.getInt(LIFE_TAG);
        if (tag.hasUUID(JUMPER_TAG)) jumperId = tag.getUUID(JUMPER_TAG);
    }

    void playFizzleSound() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4f, 1.8f);
    }
}
