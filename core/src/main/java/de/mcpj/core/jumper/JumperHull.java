package de.mcpj.core.jumper;

import de.mcpj.core.config.HullConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.MathUtil;
import de.mcpj.core.math.Vec3d;
import de.mcpj.core.util.Units;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Hull integrity: hits, drones, collisions, water and lava; the shield absorbs damage while it has energy. */
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

    /**
     * A hit as the platform sees it, already translated from the game's damage source.
     *
     * @param attacker        whoever caused it (handed back to the platform for game events), may be null
     * @param attackerOnBoard the attacker sits in this ship
     * @param friendlyDrone   a drone of this ship
     * @param drone           id of the drone that hit, or null if it was no drone
     * @param explosion       explosion damage
     * @param creativeAttacker the attacker is a player in creative mode (no item drop)
     */
    public record Damage(float amount, @Nullable Object attacker, boolean attackerOnBoard, boolean friendlyDrone,
                         @Nullable UUID drone, boolean explosion, boolean creativeAttacker) {}

    private final Jumper jumper;
    @Nullable private UUID lastDroneHit;
    private int underFireTimer;
    private int impactCooldown;

    JumperHull(Jumper jumper) {
        this.jumper = jumper;
    }

    public float integrity() {
        return jumper.sync().hullIntegrity();
    }

    private void setIntegrity(float integrity) {
        jumper.sync().setHullIntegrity(MathUtil.clamp(integrity, 0f, 1f));
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
        return MathUtil.clamp((value - start) / Math.max(end - start, 1.0e-5f), 0, 1);
    }

    void tickHurtAnimation() {
        JumperSync sync = jumper.sync();
        if (sync.hurtTime() > 0) sync.setHurtTime(sync.hurtTime() - 1);
        if (sync.hurtWobble() > 0) sync.setHurtWobble(Math.max(0, sync.hurtWobble() - 1));
    }

    void tick() {
        if (underFireTimer > 0) underFireTimer--;
        if (impactCooldown > 0) impactCooldown--;
        if (jumper.host().tickCount() % Units.TICKS_PER_SECOND != 0) return;
        if (jumper.touchesLava()) {
            burnInLava();
        } else if (jumper.isSubmerged() && !jumper.isShielded()) {
            damage(ServerConfig.HULL.waterDamagePerSecond(), null, false, false);
            spawnLeakBubbles();
        }
    }

    /** Lava (also flowing) burns the hull; an active shield absorbs it like a hit while energy lasts. */
    private void burnInLava() {
        float hullPoints = ServerConfig.HULL.lavaDamagePerSecond();
        if (hullPoints <= 0 || absorbWithShield(hullPoints)) return;
        JumperSounds.lavaBurn(jumper);
        spawnLavaSmoke();
        damage(hullPoints, null, false, false);
    }

    /** A hit; returns whether it counts as accepted (like Minecraft's hurt). */
    public boolean hurt(Damage hit) {
        if (jumper.host().isClientSide() || !jumper.host().isAlive()) return true;
        if (hit.attackerOnBoard()) return false;
        if (hit.friendlyDrone()) return false;

        float hullPoints = hit.amount();
        if (hit.drone() != null) {
            if (hit.drone().equals(lastDroneHit)) return false;
            lastDroneHit = hit.drone();
            hullPoints = droneDamage(hit);
        }
        if (absorbWithShield(hullPoints)) return false;
        damage(hullPoints, hit.attacker(), true, hit.creativeAttacker());
        return true;
    }

    private static float droneDamage(Damage hit) {
        HullConfig config = ServerConfig.HULL;
        float fullHit = config.maxHull() * config.droneHitFraction();
        return hit.explosion() ? Math.min(fullHit, hit.amount() * fullHit / DRONE_EXPLOSION_REFERENCE_DAMAGE) : fullHit;
    }

    public void onImpact(float horizontalSpeed, float fallSpeed) {
        if (jumper.host().isClientSide() || !jumper.host().isAlive() || impactCooldown > 0) return;
        double maxHorizontal = ServerConfig.FLIGHT.maxSpeedPerTick() * IMPACT_SPEED_TOLERANCE;
        double maxFall = JumperFlight.MAX_FALL_SPEED + IMPACT_FALL_TOLERANCE;
        float fraction = impactFraction(Math.min(horizontalSpeed, maxHorizontal), Math.min(fallSpeed, maxFall));
        if (fraction <= 0) return;
        impactCooldown = IMPACT_COOLDOWN_TICKS;
        JumperSounds.impact(jumper, fraction);
        float hullPoints = fraction * ServerConfig.HULL.maxHull();
        if (!absorbWithShield(hullPoints)) damage(hullPoints, null, true, false);
    }

    private boolean absorbWithShield(float hullPoints) {
        underFireTimer = ServerConfig.ENERGY.underFireTicks();
        if (!jumper.isShielded()) return false;
        if (jumper.energy().drain(Math.round(hullPoints * ServerConfig.ENERGY.shieldHitCost()))) {
            jumper.host().showShieldHit();
            JumperSounds.shieldHit(jumper);
            return true;
        }
        jumper.systems().collapseShield();
        return false;
    }

    private void damage(float hullPoints, @Nullable Object attacker, boolean feedback, boolean creativeAttacker) {
        JumperSync sync = jumper.sync();
        if (feedback) {
            sync.setHurtDirection(-sync.hurtDirection());
            sync.setHurtTime(HURT_ANIMATION_TICKS);
            sync.setHurtWobble(Math.min(MAX_WOBBLE, sync.hurtWobble() + hullPoints * WOBBLE_PER_HULL_POINT));
            JumperSounds.hullHit(jumper);
        }
        setIntegrity(integrity() - hullPoints / ServerConfig.HULL.maxHull());
        jumper.host().hullDamaged(attacker);
        if (integrity() <= 0f) destroy(creativeAttacker);
    }

    /**
     * Passengers are thrown out first (they are no longer protected by the hull), then the jumper explodes.
     * Item and cargo drop after the explosion so it does not destroy them.
     */
    private void destroy(boolean creativeAttacker) {
        JumperHost host = jumper.host();
        host.ejectPassengers();
        host.discard();
        float power = ServerConfig.HULL.destructionExplosionPower();
        if (power > 0) host.explode(host.location().add(0, JumperGeometry.HEIGHT * EXPLOSION_HEIGHT, 0), power);
        if (!creativeAttacker && host.entityDropsEnabled()) host.dropShipItem();
        host.dropCargo();
    }

    private void spawnLeakBubbles() {
        spawnParticles(JumperParticle.LEAK_BUBBLE, WATER_BUBBLE_COUNT, WATER_BUBBLE_SPEED);
    }

    private void spawnLavaSmoke() {
        spawnParticles(JumperParticle.LAVA_SMOKE, LAVA_SMOKE_COUNT, LAVA_SMOKE_SPEED);
    }

    private void spawnParticles(JumperParticle particle, int count, double speed) {
        Vec3d at = jumper.host().location().add(0, JumperGeometry.HEIGHT * WATER_BUBBLE_HEIGHT, 0);
        jumper.host().serverParticles(particle, at, count, new Vec3d(WATER_BUBBLE_SPREAD, WATER_BUBBLE_SPREAD_Y, WATER_BUBBLE_SPREAD), speed);
    }

    /** Client: the hurt animation for a hit reported by the server. */
    public void animateHurt() {
        JumperSync sync = jumper.sync();
        sync.setHurtDirection(-sync.hurtDirection());
        sync.setHurtTime(HURT_ANIMATION_TICKS);
        sync.setHurtWobble(sync.hurtWobble() * CLIENT_WOBBLE_BOOST);
    }

    public void load(SaveData data) {
        if (data.contains(INTEGRITY_TAG)) setIntegrity(data.getFloat(INTEGRITY_TAG));
        else if (data.contains(LEGACY_HEALTH_TAG)) setIntegrity(data.getFloat(LEGACY_HEALTH_TAG) / LEGACY_MAX_HEALTH);
    }

    public void save(SaveData data) {
        data.putFloat(INTEGRITY_TAG, integrity());
    }
}
