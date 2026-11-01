package de.matthias.puddlejumper.entity;

import de.matthias.puddlejumper.ModRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

/** Antiker Drohnen-Flugkörper: zielsuchend, leuchtend, durchschlägt keine Blöcke. */
public class DroneEntity extends Projectile {
    private static final int MAX_LIFE = 320;
    private static final double MAX_SPEED = 1.1;
    private static final float DAMAGE = 8f;
    /** Explosionsstärke beim Einschlag (Creeper: 3). Blöcke nur bei aktivem mobGriefing. */
    private static final float EXPLOSION_POWER = 2.0f;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(1.0f, 0.78f, 0.25f), 1.1f);

    private int life;
    @Nullable private Entity target;
    @Nullable private UUID jumperId;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public DroneEntity(Level level, LivingEntity owner, PuddleJumperEntity jumper, @Nullable Entity target) {
        this(ModRegistry.DRONE.get(), level);
        setOwner(owner);
        this.jumperId = jumper.getUUID();
        this.target = target;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();

        if (!level().isClientSide) {
            if (++life > MAX_LIFE) {
                fizzle(position());
                discard();
                return;
            }
            if ((target == null || !target.isAlive()) && life % 5 == 0) target = acquireTarget(v);
            double speed = Math.min(MAX_SPEED, v.length() + 0.035);
            Vec3 dir = v.lengthSqr() > 1.0e-6 ? v.normalize() : new Vec3(0, 0, 1);
            if (target != null && target.isAlive()) {
                Vec3 delta = target.getBoundingBox().getCenter().subtract(position());
                Vec3 to = delta.normalize();
                // träge Lenkung auf Distanz, im Nahbereich stärker (sonst kreist die Drohne ums Ziel)
                double near = Mth.clamp(1.0 - delta.length() / 10.0, 0.0, 1.0);
                double turn = life < 8 ? 0.04 : 0.12 + 0.35 * near;
                dir = dir.lerp(to, turn).normalize();
                // in engen Kurven abbremsen
                speed *= 0.75 + 0.25 * Math.max(0, dir.dot(to));
            }
            v = dir.scale(speed);
            setDeltaMovement(v);

            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
                onHit(hit);
                if (isRemoved()) return;
            }
        } else {
            for (int i = 0; i < 2; i++) {
                double t = random.nextDouble();
                level().addParticle(TRAIL, getX() - v.x * t, getY() + 0.15 - v.y * t, getZ() - v.z * t, 0, 0, 0);
            }
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.15, getZ(), 0, 0, 0);
            }
        }

        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.5f);
    }

    @Nullable
    private Entity acquireTarget(Vec3 v) {
        Vec3 dir = v.lengthSqr() > 1.0e-6 ? v.normalize() : Vec3.ZERO;
        Vec3 c = position();
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(28), e ->
                        e instanceof Enemy && e.isAlive() && canHitEntity(e)
                                && e.position().subtract(c).normalize().dot(dir) > 0.2)
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !(e instanceof DroneEntity) && !isFriendly(e);
    }

    /** Eigener Jumper (inkl. Bug/Heck), Pilot und Mitflieger - werden weder getroffen noch von der Explosion verletzt. */
    public boolean isFriendly(Entity e) {
        if (e instanceof PuddleJumperEntity j && j.getUUID().equals(jumperId)) return true;
        if (e instanceof PuddleJumperPart p && p.getParent().getUUID().equals(jumperId)) return true;
        if (e.getVehicle() instanceof PuddleJumperEntity j && j.getUUID().equals(jumperId)) return true;
        Entity owner = getOwner();
        return owner != null && (e == owner || e.isPassengerOfSameVehicle(owner));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        hit.hurt(damageSources().thrown(this, getOwner()), DAMAGE);
        Vec3 push = getDeltaMovement().normalize().scale(0.6);
        hit.push(push.x, 0.25, push.z);
        explode(result.getLocation());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explode(result.getLocation());
        discard();
    }

    /** Echte Explosion (wie Creeper, etwas kleiner) plus goldene Funken. */
    private void explode(Vec3 at) {
        if (level() instanceof ServerLevel sl) {
            sl.explode(this, at.x, at.y, at.z, EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
            sl.sendParticles(TRAIL, at.x, at.y, at.z, 24, 0.5, 0.5, 0.5, 0.05);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 16, 0.4, 0.4, 0.4, 0.4);
        }
    }

    /** Lebensdauer abgelaufen: verpufft ohne Explosion. */
    private void fizzle(Vec3 at) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(TRAIL, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.03);
            sl.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4f, 1.8f);
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", life);
        if (jumperId != null) tag.putUUID("Jumper", jumperId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = tag.getInt("Life");
        if (tag.hasUUID("Jumper")) jumperId = tag.getUUID("Jumper");
    }
}
