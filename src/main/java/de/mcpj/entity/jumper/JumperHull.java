package de.mcpj.entity.jumper;

import de.mcpj.config.HullConfig;
import de.mcpj.config.ServerConfig;
import de.mcpj.entity.drone.DroneEntity;
import de.mcpj.registry.ModItems;
import de.mcpj.util.Units;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static de.mcpj.entity.jumper.PuddleJumperEntity.HULL_INTEGRITY;
import static de.mcpj.entity.jumper.PuddleJumperEntity.HURT_DIRECTION;
import static de.mcpj.entity.jumper.PuddleJumperEntity.HURT_TIME;
import static de.mcpj.entity.jumper.PuddleJumperEntity.HURT_WOBBLE;

public final class JumperHull {
    private static final int HURT_ANIMATION_TICKS = 10;
    private static final float WOBBLE_PER_HULL_POINT = 10f;
    private static final float MAX_WOBBLE = 200f;
    private static final float CLIENT_WOBBLE_BOOST = 11f;
    private static final float DRONE_EXPLOSION_REFERENCE_DAMAGE = 25f;
    private static final int IMPACT_COOLDOWN_TICKS = 5;
    private static final double IMPACT_SPEED_TOLERANCE = 1.2;
    private static final double IMPACT_FALL_TOLERANCE = 0.5;
    private static final int WATER_BUBBLE_COUNT = 12;
    private static final double WATER_BUBBLE_HEIGHT = 0.7;
    private static final double WATER_BUBBLE_SPREAD = 1.2 * JumperGeometry.RENDER_SCALE;
    private static final double WATER_BUBBLE_SPREAD_Y = 0.4 * JumperGeometry.RENDER_SCALE;
    private static final double WATER_BUBBLE_SPEED = 0.05;
    private static final int LAVA_SMOKE_COUNT = 10;
    private static final double LAVA_SMOKE_SPEED = 0.02;
    private static final double EXPLOSION_HEIGHT = 0.5;
    private static final String INTEGRITY_TAG = "Hull";
    private static final String LEGACY_HEALTH_TAG = "Health";
    private static final float LEGACY_MAX_HEALTH = 240f;

    private final PuddleJumperEntity jumper;
    @Nullable private UUID lastDroneHit;
    private int underFireTimer;
    private int impactCooldown;

    JumperHull(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    public float integrity() {
        return jumper.getEntityData().get(HULL_INTEGRITY);
    }

    private void setIntegrity(float integrity) {
        jumper.getEntityData().set(HULL_INTEGRITY, Mth.clamp(integrity, 0f, 1f));
    }

    public boolean isUnderFire() {
        return underFireTimer > 0;
    }

    public static float impactFraction(double horizontalSpeed, double fallSpeed) {
        HullConfig config = ServerConfig.HULL;
        double horizontal = ramp(horizontalSpeed, config.impactSafeSpeedPerTick(), config.impactFullSpeedPerTick());
        double vertical = ramp(fallSpeed, config.impactSafeFallSpeedPerTick(), config.impactFullFallSpeedPerTick());
        return (float) Math.max(horizontal, vertical) * config.maxImpactFraction();
    }

    private static double ramp(double value, double start, double end) {
        return Mth.clamp((value - start) / Math.max(end - start, Mth.EPSILON), 0, 1);
    }

    void tickHurtAnimation() {
        if (jumper.hurtTime() > 0) jumper.getEntityData().set(HURT_TIME, jumper.hurtTime() - 1);
        if (jumper.hurtWobble() > 0) jumper.getEntityData().set(HURT_WOBBLE, Math.max(0, jumper.hurtWobble() - 1));
    }

    void tick() {
        if (underFireTimer > 0) underFireTimer--;
        if (impactCooldown > 0) impactCooldown--;
        if (jumper.tickCount % Units.TICKS_PER_SECOND != 0) return;
        if (jumper.touchesLava()) {
            burnInLava();
        } else if (jumper.isSubmerged() && !jumper.isShielded()) {
            damage(ServerConfig.HULL.waterDamagePerSecond(), null, false);
            spawnLeakBubbles();
        }
    }

    /** Lava (also flowing) burns the hull; an active shield absorbs it like a hit while energy lasts. */
    private void burnInLava() {
        float hullPoints = ServerConfig.HULL.lavaDamagePerSecond();
        if (hullPoints <= 0 || absorbWithShield(hullPoints)) return;
        JumperSounds.lavaBurn(jumper);
        spawnLavaSmoke();
        damage(hullPoints, null, false);
    }

    boolean hurt(DamageSource source, float amount) {
        if (jumper.isInvulnerableTo(source)) return false;
        if (jumper.level().isClientSide || jumper.isRemoved()) return true;
        Entity attacker = source.getEntity();
        if (attacker != null && jumper.hasPassenger(attacker)) return false;
        if (jumper.isFriendlyDrone(source)) return false;

        float hullPoints = amount;
        if (source.getDirectEntity() instanceof DroneEntity drone) {
            if (drone.getUUID().equals(lastDroneHit)) return false;
            lastDroneHit = drone.getUUID();
            hullPoints = droneDamage(source, amount);
        }
        if (absorbWithShield(hullPoints)) return false;
        damage(hullPoints, attacker, true);
        return true;
    }

    private static float droneDamage(DamageSource source, float amount) {
        HullConfig config = ServerConfig.HULL;
        float fullHit = config.maxHull() * config.droneHitFraction();
        return source.is(DamageTypeTags.IS_EXPLOSION) ? Math.min(fullHit, amount * fullHit / DRONE_EXPLOSION_REFERENCE_DAMAGE) : fullHit;
    }

    public void onImpact(float horizontalSpeed, float fallSpeed) {
        if (jumper.level().isClientSide || jumper.isRemoved() || impactCooldown > 0) return;
        double maxHorizontal = ServerConfig.FLIGHT.maxSpeedPerTick() * IMPACT_SPEED_TOLERANCE;
        double maxFall = JumperFlight.MAX_FALL_SPEED + IMPACT_FALL_TOLERANCE;
        float fraction = impactFraction(Math.min(horizontalSpeed, maxHorizontal), Math.min(fallSpeed, maxFall));
        if (fraction <= 0) return;
        impactCooldown = IMPACT_COOLDOWN_TICKS;
        JumperSounds.impact(jumper, fraction);
        float hullPoints = fraction * ServerConfig.HULL.maxHull();
        if (!absorbWithShield(hullPoints)) damage(hullPoints, null, true);
    }

    private boolean absorbWithShield(float hullPoints) {
        underFireTimer = ServerConfig.ENERGY.underFireTicks();
        if (!jumper.isShielded()) return false;
        if (jumper.energy().drain(Math.round(hullPoints * ServerConfig.ENERGY.shieldHitCost()))) {
            jumper.level().broadcastEntityEvent(jumper, PuddleJumperEntity.SHIELD_HIT_EVENT);
            JumperSounds.shieldHit(jumper);
            return true;
        }
        jumper.systems().collapseShield();
        return false;
    }

    private void damage(float hullPoints, @Nullable Entity attacker, boolean feedback) {
        if (feedback) {
            jumper.getEntityData().set(HURT_DIRECTION, -jumper.hurtDirection());
            jumper.getEntityData().set(HURT_TIME, HURT_ANIMATION_TICKS);
            jumper.getEntityData().set(HURT_WOBBLE, Math.min(MAX_WOBBLE, jumper.hurtWobble() + hullPoints * WOBBLE_PER_HULL_POINT));
            JumperSounds.hullHit(jumper);
        }
        setIntegrity(integrity() - hullPoints / ServerConfig.HULL.maxHull());
        jumper.gameEvent(GameEvent.ENTITY_DAMAGE, attacker);
        if (integrity() <= 0f) destroy(attacker);
    }

    /**
     * Passengers are thrown out first (they are no longer protected by the hull), then the jumper explodes.
     * Item and cargo drop after the explosion so it does not destroy them.
     */
    private void destroy(@Nullable Entity attacker) {
        jumper.ejectPassengers();
        jumper.discard();
        float power = ServerConfig.HULL.destructionExplosionPower();
        if (power > 0) {
            Vec3 at = jumper.position().add(0, JumperGeometry.HEIGHT * EXPLOSION_HEIGHT, 0);
            jumper.level().explode(jumper, at.x, at.y, at.z, power, Level.ExplosionInteraction.MOB);
        }
        boolean creative = attacker instanceof Player player && player.getAbilities().instabuild;
        if (!creative && jumper.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            jumper.spawnAtLocation(ModItems.PUDDLE_JUMPER.get());
        }
        jumper.cargo().dropAll();
    }

    private void spawnLavaSmoke() {
        if (!(jumper.level() instanceof ServerLevel level)) return;
        Vec3 at = jumper.position().add(0, JumperGeometry.HEIGHT * WATER_BUBBLE_HEIGHT, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, LAVA_SMOKE_COUNT,
                WATER_BUBBLE_SPREAD, WATER_BUBBLE_SPREAD_Y, WATER_BUBBLE_SPREAD, LAVA_SMOKE_SPEED);
    }

    private void spawnLeakBubbles() {
        if (!(jumper.level() instanceof ServerLevel level)) return;
        Vec3 at = jumper.position().add(0, JumperGeometry.HEIGHT * WATER_BUBBLE_HEIGHT, 0);
        level.sendParticles(ParticleTypes.BUBBLE, at.x, at.y, at.z, WATER_BUBBLE_COUNT,
                WATER_BUBBLE_SPREAD, WATER_BUBBLE_SPREAD_Y, WATER_BUBBLE_SPREAD, WATER_BUBBLE_SPEED);
    }

    void animateHurt() {
        jumper.getEntityData().set(HURT_DIRECTION, -jumper.hurtDirection());
        jumper.getEntityData().set(HURT_TIME, HURT_ANIMATION_TICKS);
        jumper.getEntityData().set(HURT_WOBBLE, jumper.hurtWobble() * CLIENT_WOBBLE_BOOST);
    }

    void load(CompoundTag tag) {
        if (tag.contains(INTEGRITY_TAG)) setIntegrity(tag.getFloat(INTEGRITY_TAG));
        else if (tag.contains(LEGACY_HEALTH_TAG)) setIntegrity(tag.getFloat(LEGACY_HEALTH_TAG) / LEGACY_MAX_HEALTH);
    }

    void save(CompoundTag tag) {
        tag.putFloat(INTEGRITY_TAG, integrity());
    }
}
