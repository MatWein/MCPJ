package de.mcpj.entity.drone;

import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.drone.Drone;
import de.mcpj.core.drone.DroneHost;
import de.mcpj.core.drone.DroneParticle;
import de.mcpj.core.math.Vec3d;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import de.mcpj.entity.jumper.PuddleJumperPart;
import de.mcpj.registry.ModEntities;
import de.mcpj.util.Convert;
import de.mcpj.util.NbtSaveData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * The drone in Minecraft 1.20.1 / Forge: steering, lifetime and effects come from the core ({@link Drone});
 * this projectile handles hits, explosions, target search and who counts as friendly.
 */
public class DroneEntity extends Projectile implements DroneHost {
    public static final float SIZE = 0.5f;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(1.0f, 0.78f, 0.25f), 1.1f);
    private static final float ROTATION_SMOOTHING = 0.5f;
    private static final double RENDER_DISTANCE = 128;
    private static final String JUMPER_TAG = "Jumper";

    private final Drone core = new Drone(this);
    private final RandomGenerator randomGenerator = () -> random.nextLong();
    @Nullable private UUID jumperId;
    @Nullable private Entity target;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public DroneEntity(Level level, LivingEntity owner, PuddleJumperEntity jumper, @Nullable Entity target) {
        this(ModEntities.DRONE.get(), level);
        setOwner(owner);
        jumperId = jumper.getUUID();
        this.target = target;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            core.clientTick();
        } else if (!serverTick()) {
            return;
        }
        Vec3 velocity = getDeltaMovement();
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        ProjectileUtil.rotateTowardsMovement(this, ROTATION_SMOOTHING);
    }

    private boolean serverTick() {
        if (!core.serverTick()) {
            discard();
            return false;
        }
        setDeltaMovement(Convert.toMc(core.steer()));
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            onHit(hit);
        }
        return !isRemoved();
    }

    // ------------------------------------------------------------------ DroneHost

    @Override
    public Vec3d location() {
        return Convert.toCore(position());
    }

    @Override
    public Vec3d velocity() {
        return Convert.toCore(getDeltaMovement());
    }

    @Override
    public boolean inWater() {
        return isInWater();
    }

    @Override
    public RandomGenerator random() {
        return randomGenerator;
    }

    @Override
    @Nullable
    public Vec3d liveTargetCenter() {
        return target != null && target.isAlive() ? Convert.toCore(target.getBoundingBox().getCenter()) : null;
    }

    @Override
    public void acquireTarget(Vec3d direction, double range, double minAlignment) {
        Vec3 ahead = Convert.toMc(direction);
        Vec3 origin = position();
        target = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range),
                        entity -> entity instanceof Enemy && entity.isAlive() && canHitEntity(entity)
                                && entity.position().subtract(origin).normalize().dot(ahead) > minAlignment)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this)))
                .orElse(null);
    }

    @Override
    public void clientParticle(DroneParticle particle, Vec3d at, Vec3d velocity) {
        level().addParticle(particle(particle), at.x(), at.y(), at.z(), velocity.x(), velocity.y(), velocity.z());
    }

    @Override
    public void serverParticles(DroneParticle particle, Vec3d at, int count, double spread, double speed) {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(particle(particle), at.x(), at.y(), at.z(), count, spread, spread, spread, speed);
        }
    }

    private static ParticleOptions particle(DroneParticle particle) {
        return switch (particle) {
            case TRAIL -> TRAIL;
            case BUBBLE -> ParticleTypes.BUBBLE;
            case SPARK -> ParticleTypes.ELECTRIC_SPARK;
        };
    }

    @Override
    public void playFizzleSound() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4f, 1.8f);
    }

    // ------------------------------------------------------------------ hits

    public RandomSource getRandom() {
        return random;
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
        hit.hurt(damageSources().thrown(this, getOwner()), ServerConfig.DRONES.damage());
        Vec3d push = Drone.knockback(velocity());
        hit.push(push.x(), Drone.knockbackLift(), push.z());
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
            core.explosionEffects(Convert.toCore(at));
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
        core.save(new NbtSaveData(tag));
        if (jumperId != null) tag.putUUID(JUMPER_TAG, jumperId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        core.load(new NbtSaveData(tag));
        if (tag.hasUUID(JUMPER_TAG)) jumperId = tag.getUUID(JUMPER_TAG);
    }
}
