package de.matthias.puddlejumper.entity;

import de.matthias.puddlejumper.Config;
import de.matthias.puddlejumper.ModRegistry;
import de.matthias.puddlejumper.block.ChargingPadBlockEntity;
import de.matthias.puddlejumper.network.JumperImpactPacket;
import de.matthias.puddlejumper.network.ModNetwork;
import de.matthias.puddlejumper.network.JumperActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.function.Consumer;

/**
 * Der Puddle Jumper: ein steuerbares Fluggerät mit Tarnung, Heckklappe samt Laderaum und Drohnenwerfern.
 *
 * <p>Wie bei Booten simuliert der Client des Piloten die Bewegung und schickt die Position an den Server.
 * Ohne Pilot simuliert der Server (Schwerkraft, sanftes Absinken).</p>
 */
public class PuddleJumperEntity extends Entity implements HasCustomInventoryScreen {
    /**
     * Sichtbare Größe des Schiffs (1 = ursprüngliche 8 Blöcke Länge): Modell, Schild-/Tarnblase, Schatten,
     * Kamera, Pilotensitz, Drohnen-Startpunkte und Aussteigepunkte skalieren damit.
     */
    public static final float RENDER_SIZE = 1.3f;
    /** Größe der Hitbox (Kollision, Treffer) - bewusst kleiner als das Modell. */
    public static final float HITBOX_SIZE = 1.1f;
    public static final float WIDTH = 2.6f * HITBOX_SIZE;
    public static final float HEIGHT = 2.0f * HITBOX_SIZE;
    /** Modell-Einheiten (OBJ) -> Blöcke. Das Modell ist 28.9 Einheiten lang -> 8 Blöcke * RENDER_SIZE. */
    public static final float MODEL_SCALE = 8.0f * RENDER_SIZE / 28.9f;
    /** Abstand der Bug-/Heck-Hitbox vom Mittelpunkt (Blöcke). */
    public static final double SEGMENT_OFFSET = 2.7 * HITBOX_SIZE;

    // Flugwerte (Blöcke/Tick)
    private static final double ACCEL = 0.022;
    private static final double MAX_SPEED = 1.0;
    /** Drehung mit A/D: Winkelbeschleunigung (°/Tick²) und Dämpfung -> max. ~2.5°/Tick. */
    private static final float TURN_ACCEL = 0.3f, TURN_DRAG = 0.88f;
    /** Luftwiderstand mit Schub (Zeitkonstante ~2 s) bzw. beim Ausrollen (~1 s). */
    private static final double DRAG_THRUST = 0.975, DRAG_IDLE = 0.95;

    public static final int DRONE_COOLDOWN = 15;
    /** Gondel-Position im Modell (muss zu tools/build_model.py passen). */
    public static final float POD_X = 4.95f, POD_Y = 4.3f, POD_REAR_Z = -12.2f;
    /** Hüllenintegrität: Diamantschwert (7) braucht 35 normale bzw. ~23 kritische Schläge. */
    public static final float MAX_HEALTH = 240f;
    /** Ein (fremder) Drohnentreffer kostet 60 % der Hülle: die erste übersteht er knapp, die zweite nicht. */
    private static final float DRONE_HIT_FRACTION = 0.6f;
    /** Unter Wasser: Faktor für Schub/Drehung, eigene Dämpfung, Hüllenschaden pro Sekunde (ohne Schild). */
    private static final double WATER_ACCEL = 0.5, WATER_DRAG_THRUST = 0.955, WATER_DRAG_IDLE = 0.92;
    private static final float WATER_DAMAGE = 1f;

    // Ohne Energie: freier Fall (Blöcke/Tick²), Luftwiderstand, Endgeschwindigkeit; unter Wasser sinkt er langsamer
    private static final double GRAVITY = 0.08, FALL_DRAG = 0.98, MAX_FALL = 3.0;
    private static final double WATER_GRAVITY = 0.02, WATER_FALL_DRAG = 0.9;
    /**
     * Aufprall (verlorenes Tempo in Blöcken/Tick): bis SAFE nichts, ab FULL der Höchstschaden {@link #MAX_IMPACT_FRACTION}.
     * Nach unten großzügiger, damit normales Landen mit voller Sinkrate (~0,7) folgenlos bleibt.
     */
    private static final double IMPACT_H_SAFE = 0.4, IMPACT_H_FULL = 1.0;
    private static final double IMPACT_DOWN_SAFE = 0.75, IMPACT_DOWN_FULL = 1.6;
    public static final float MAX_IMPACT_FRACTION = 0.3f;
    /** So lange (Ticks) nach einem Treffer gilt der Jumper als "unter Beschuss". */
    private static final int UNDER_FIRE_TICKS = 40;
    /** Alle so viele Ticks zieht der gelandete Jumper Strom aus den Ladestationen. */
    private static final int CHARGE_INTERVAL = 5;

    private static final EntityDataAccessor<Boolean> DATA_CLOAKED = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHIELD = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LIGHTS = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PODS_OUT = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_HURT_TIME = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HURT_DIR = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CAPACITY = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);
    /** Netto-Energiefluss (FE/Tick, geglättet): positiv = Verbrauch, negativ = Laden. Nur für die Anzeige. */
    private static final EntityDataAccessor<Integer> DATA_FLOW = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);

    private static final DustParticleOptions ION = new DustParticleOptions(new Vector3f(0.45f, 0.75f, 1.0f), 0.6f);

    /** Client-Hook (Triebwerkssound); wird vom Client-Setup gesetzt. */
    public static Consumer<PuddleJumperEntity> clientTicker = j -> {};

    private final PuddleJumperPart front;
    private final PuddleJumperPart rear;
    private final PuddleJumperPart[] parts;

    private final CargoContainer cargo = new CargoContainer();

    // Interpolation für nicht lokal gesteuerte Instanzen
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot;

    // Server-Zustand
    /** Vom Piloten per Taste erzwungener Gondel-Zustand; null = automatisch (gelandet = eingefahren). */
    @Nullable private Boolean podsOverride;
    private boolean lastLanded;
    private int podsTimer;
    private long lastDroneTick = -100;
    private boolean leftLauncher;
    private Vec3 lastServerPos = Vec3.ZERO;
    private Vec3 serverVelocity = Vec3.ZERO;
    /** Energie schon gesetzt (aus Spielstand oder Startladung)? */
    private boolean energyInitialized;
    private int underFireTimer;
    private int impactCooldown;
    private int lastChargePerTick;
    private float flowAvg;

    // Client-Animation
    public float podAnim, podAnimO;
    public float visRoll, visRollO;
    public float visPitch, visPitchO;
    public double clientSpeed;
    /** 0..1, wie stark die Düsen glühen (schwebend/fliegend an, gelandet aus). */
    public float engineGlow, engineGlowO;
    private float yawVelocity;
    /** Selbst berechnete Geschwindigkeit; Fremdimpulse (Explosionen, Stöße, Treffer) werden damit verworfen. */
    private Vec3 ownVelocity = Vec3.ZERO;
    @Nullable private java.util.UUID lastDroneHit;
    /** Einblend-Animation Schild/Tarnung (0..1) und kurzes Aufleuchten bei Treffern. */
    public float shieldAnim, shieldAnimO, cloakAnim, cloakAnimO;
    public int shieldFlash;
    private static final byte EVENT_SHIELD_HIT = 60;

    public PuddleJumperEntity(EntityType<? extends PuddleJumperEntity> type, Level level) {
        super(type, level);
        this.front = new PuddleJumperPart(this, WIDTH, HEIGHT);
        this.rear = new PuddleJumperPart(this, WIDTH, HEIGHT);
        this.parts = new PuddleJumperPart[]{front, rear};
        this.setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        this.setNoGravity(true); // eigene Schwerkraft; verhindert außerdem den "Floating"-Kick am Server
        this.blocksBuilding = true;
        this.setMaxUpStep(1.0f);
    }

    // ------------------------------------------------------------------ Sync-Daten

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_CLOAKED, false);
        entityData.define(DATA_SHIELD, false);
        entityData.define(DATA_PODS_OUT, false);
        entityData.define(DATA_LIGHTS, false);
        entityData.define(DATA_HURT_TIME, 0);
        entityData.define(DATA_HURT_DIR, 1);
        entityData.define(DATA_DAMAGE, 0f);
        entityData.define(DATA_HEALTH, MAX_HEALTH);
        entityData.define(DATA_ENERGY, 0);
        entityData.define(DATA_CAPACITY, 1);
        entityData.define(DATA_FLOW, 0);
    }

    public int getEnergy() { return entityData.get(DATA_ENERGY); }
    public void setEnergy(int v) {
        energyInitialized = true;
        entityData.set(DATA_ENERGY, Mth.clamp(v, 0, getCapacity()));
    }
    public int getCapacity() { return Math.max(1, entityData.get(DATA_CAPACITY)); }
    public float getEnergyFraction() { return getEnergy() / (float) getCapacity(); }
    public boolean hasEnergy() { return getEnergy() > 0; }
    /** Geglätteter Netto-Fluss in FE/Tick (positiv = Verbrauch, negativ = Laden). */
    public int getEnergyFlow() { return entityData.get(DATA_FLOW); }

    public boolean isCloaked() { return entityData.get(DATA_CLOAKED); }
    public void setCloaked(boolean v) { entityData.set(DATA_CLOAKED, v); }
    public boolean isShielded() { return entityData.get(DATA_SHIELD); }
    public void setShielded(boolean v) { entityData.set(DATA_SHIELD, v); }
    public boolean isLightsOn() { return entityData.get(DATA_LIGHTS); }
    public void setLightsOn(boolean v) { entityData.set(DATA_LIGHTS, v); }
    public boolean isPodsOut() { return entityData.get(DATA_PODS_OUT); }

    /** Triebwerke laufen: mit Energie, in der Luft (auch ohne Pilot) oder mit Pilot an Bord. */
    public boolean enginesRunning() {
        return hasEnergy() && (isVehicle() || !onGround());
    }
    public int getHurtTime() { return entityData.get(DATA_HURT_TIME); }
    public void setHurtTime(int v) { entityData.set(DATA_HURT_TIME, v); }
    public int getHurtDir() { return entityData.get(DATA_HURT_DIR); }
    public void setHurtDir(int v) { entityData.set(DATA_HURT_DIR, v); }
    public float getHealth() { return entityData.get(DATA_HEALTH); }
    public void setHealth(float v) { entityData.set(DATA_HEALTH, Mth.clamp(v, 0f, MAX_HEALTH)); }
    /** Nur für die Wackel-Animation bei Treffern (klingt ab). */
    public float getDamage() { return entityData.get(DATA_DAMAGE); }
    public void setDamage(float v) { entityData.set(DATA_DAMAGE, v); }

    // ------------------------------------------------------------------ Multipart

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    /** Horizontaler Vorwärtsvektor (Bug-Richtung). */
    public Vec3 forward() {
        float r = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    /** Horizontaler Vektor nach links (entspricht Modell +X). */
    public Vec3 left() {
        float r = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(r), 0, Mth.sin(r));
    }

    /** Modellkoordinaten (OBJ-Einheiten) in Weltkoordinaten umrechnen. */
    public Vec3 modelToWorld(double mx, double my, double mz) {
        Vec3 f = forward(), l = left();
        double s = MODEL_SCALE;
        return position().add(l.scale(mx * s)).add(f.scale(mz * s)).add(0, my * s, 0);
    }

    private void updateParts() {
        Vec3 f = forward().scale(SEGMENT_OFFSET);
        movePart(front, getX() + f.x, getY(), getZ() + f.z);
        movePart(rear, getX() - f.x, getY(), getZ() - f.z);
    }

    private static void movePart(PuddleJumperPart part, double x, double y, double z) {
        part.xo = part.getX();
        part.yo = part.getY();
        part.zo = part.getZ();
        part.xOld = part.getX();
        part.yOld = part.getY();
        part.zOld = part.getZ();
        part.setPos(x, y, z);
    }

    private AABB segmentBox(double offset) {
        Vec3 f = forward().scale(offset);
        double hw = WIDTH / 2.0;
        return new AABB(getX() + f.x - hw, getY(), getZ() + f.z - hw, getX() + f.x + hw, getY() + HEIGHT, getZ() + f.z + hw);
    }

    /** Hitbox des ganzen Schiffs (Bug, Mitte, Heck). */
    public AABB fullBoundingBox() {
        return getBoundingBox().minmax(segmentBox(SEGMENT_OFFSET)).minmax(segmentBox(-SEGMENT_OFFSET));
    }

    /** Kann dieser Jumper das Ziel erfassen? Lebewesen und andere (ungetarnte) Jumper, nicht sich selbst/Insassen. */
    public boolean isValidTarget(@Nullable Entity e) {
        if (e == null || !e.isAlive() || e.isRemoved() || e.isSpectator() || e == this || hasPassenger(e)) return false;
        if (e instanceof PuddleJumperEntity other) return !other.isCloaked();
        return e instanceof LivingEntity;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.5, 1.0, 4.5);
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void tick() {
        if (!level().isClientSide) initEnergy();
        if (getHurtTime() > 0) setHurtTime(getHurtTime() - 1);
        if (getDamage() > 0) setDamage(Math.max(0, getDamage() - 1));

        super.tick();
        tickLerp();
        setDeltaMovement(ownVelocity); // keinerlei Rückstoß von außen

        if (isControlledByLocalInstance()) {
            if (getControllingPassenger() instanceof Player pilot) {
                flyControlled(pilot);
            } else {
                idlePhysics();
            }
            moveWithSegments(getDeltaMovement());
            ownVelocity = getDeltaMovement();
        } else {
            setDeltaMovement(Vec3.ZERO);
            ownVelocity = Vec3.ZERO;
        }

        updateParts();
        setOnGround(probeGround());

        if (level().isClientSide) {
            clientTick();
        } else {
            serverTick();
        }
    }

    /**
     * Steuerung unabhängig von der Maus: W/S Schub entlang der Bugrichtung, A/D drehen das Schiff,
     * Leertaste/Strg steigen/sinken. Die Blickrichtung des Piloten bleibt beim Drehen stehen.
     */
    private void flyControlled(Player pilot) {
        if (!hasEnergy()) {        // Speicher leer: Antrieb aus, Steuerung tot -> Absturz
            fallPhysics();
            return;
        }
        FlightInput in = FlightInput.source.get();
        boolean water = isSubmerged();
        double accel = water ? ACCEL * WATER_ACCEL : ACCEL;

        yawVelocity = yawVelocity * TURN_DRAG - in.turn() * TURN_ACCEL * (water ? 0.5f : 1f); // A (turn > 0) = links
        if (in.turn() == 0 && Math.abs(yawVelocity) < 0.02f) yawVelocity = 0;
        setYRot(getYRot() + yawVelocity); // nur das Schiff dreht, die Blickrichtung des Piloten bleibt

        double fwdAccel = in.forward() > 0 ? accel : accel * 0.6;
        Vec3 acc = forward().scale(in.forward() * fwdAccel)
                .add(left().scale(in.strafe() * accel * 0.7))          // Q/E: seitlich ohne Drehung
                .add(0, in.vertical() * accel * 0.8, 0);

        boolean idle = in.forward() == 0 && in.vertical() == 0 && in.strafe() == 0;
        double drag = water ? (idle ? WATER_DRAG_IDLE : WATER_DRAG_THRUST) : (idle ? DRAG_IDLE : DRAG_THRUST);
        Vec3 v = getDeltaMovement().scale(drag).add(acc);
        if (v.lengthSqr() > MAX_SPEED * MAX_SPEED) v = v.normalize().scale(MAX_SPEED);
        if (idle && v.lengthSqr() < 1.0e-5) v = Vec3.ZERO;
        setDeltaMovement(v);
    }

    /** Ohne Pilot: Schwebe-Modus. Der Jumper rollt aus und bleibt stehen - in der Luft wie unter Wasser. */
    private void idlePhysics() {
        if (!hasEnergy() && !onGround()) {
            fallPhysics();
            return;
        }
        Vec3 v = getDeltaMovement().scale(onGround() ? 0.6 : isSubmerged() ? WATER_DRAG_IDLE : DRAG_IDLE);
        setDeltaMovement(v.lengthSqr() < 1.0e-5 ? Vec3.ZERO : v);
    }

    /** Ohne Energie: Schwerkraft wie bei einem Stein, unter Wasser sinkt er gebremst. */
    private void fallPhysics() {
        Vec3 v = getDeltaMovement();
        if (isSubmerged()) {
            v = v.scale(WATER_FALL_DRAG).add(0, -WATER_GRAVITY, 0);
        } else {
            double h = onGround() ? 0.6 : FALL_DRAG;
            v = new Vec3(v.x * h, v.y * FALL_DRAG - GRAVITY, v.z * h);
        }
        setDeltaMovement(v.x, Math.max(v.y, -MAX_FALL), v.z);
    }

    /** Mindestens zur Hälfte unter Wasser. */
    public boolean isSubmerged() {
        return getFluidHeight(net.minecraft.tags.FluidTags.WATER) > HEIGHT * 0.5;
    }

    /** Bewegung mit Kollision aller drei Segmente (Mitte über move(), Bug/Heck vorab geprüft). */
    private void moveWithSegments(Vec3 delta) {
        if (delta.lengthSqr() < 1.0e-8) {
            move(MoverType.SELF, Vec3.ZERO);
            return;
        }
        Vec3 d = delta;
        for (double off : new double[]{SEGMENT_OFFSET, -SEGMENT_OFFSET}) {
            Vec3 c = Entity.collideBoundingBox(this, d, segmentBox(off), level(), List.of());
            d = new Vec3(absMin(d.x, c.x), absMin(d.y, c.y), absMin(d.z, c.z));
        }
        Vec3 before = position();
        move(MoverType.SELF, d);
        Vec3 moved = position().subtract(before);
        Vec3 v = getDeltaMovement();
        setDeltaMovement(d.x != delta.x ? 0 : v.x, d.y != delta.y ? 0 : v.y, d.z != delta.z ? 0 : v.z);
        detectImpact(delta, moved);
    }

    /**
     * Aufprall erkennen: das Tempo auf jeder blockierten Achse geht verloren (= Aufprallgeschwindigkeit).
     * Streifen an einer Wand kostet nur den Anteil quer zur Wand; Stufen hochsteigen zählt nicht.
     */
    private void detectImpact(Vec3 wanted, Vec3 moved) {
        // Achse blockiert -> das Tempo in dieser Richtung geht beim Aufschlag komplett verloren
        double lx = Math.abs(wanted.x - moved.x) > 1.0e-4 ? wanted.x : 0;
        double lz = Math.abs(wanted.z - moved.z) > 1.0e-4 ? wanted.z : 0;
        double lostH = Math.sqrt(lx * lx + lz * lz);
        double down = wanted.y < 0 && moved.y - wanted.y > 1.0e-4 ? -wanted.y : 0;
        if (lostH <= IMPACT_H_SAFE && down <= IMPACT_DOWN_SAFE) return;
        if (level().isClientSide) {
            ModNetwork.sendToServer(new JumperImpactPacket((float) lostH, (float) down)); // Server verbucht
        } else {
            onImpact((float) lostH, (float) down);
        }
    }

    /** Anteil der Hülle, den ein Aufprall kostet (0 .. {@link #MAX_IMPACT_FRACTION}). */
    public static float impactFraction(double lostH, double down) {
        double h = Mth.clamp((lostH - IMPACT_H_SAFE) / (IMPACT_H_FULL - IMPACT_H_SAFE), 0, 1);
        double v = Mth.clamp((down - IMPACT_DOWN_SAFE) / (IMPACT_DOWN_FULL - IMPACT_DOWN_SAFE), 0, 1);
        return (float) Math.max(h, v) * MAX_IMPACT_FRACTION;
    }

    /** Server: Aufprall verbuchen (Werte vom Client werden auf plausible Grenzen gekappt). */
    public void onImpact(float lostH, float down) {
        if (level().isClientSide || isRemoved() || impactCooldown > 0) return;
        float frac = impactFraction(Math.min(lostH, MAX_SPEED * 1.2), Math.min(down, MAX_FALL + 0.5));
        if (frac <= 0) return;
        impactCooldown = 5;
        float amount = frac * MAX_HEALTH;
        playSound(SoundEvents.GENERIC_EXPLODE, 0.5f + frac * 2f, 1.4f);
        playSound(SoundEvents.ANVIL_LAND, 0.8f, 0.5f);
        if (shieldAbsorbs(amount)) return;
        applyHullDamage(amount, null, true);
    }

    /** Steht der Jumper auf etwas? (dünne Schicht unter Bug, Mitte und Heck) */
    private boolean probeGround() {
        for (double off : new double[]{SEGMENT_OFFSET, 0, -SEGMENT_OFFSET}) {
            AABB b = segmentBox(off);
            AABB slab = new AABB(b.minX + 0.05, b.minY - 0.08, b.minZ + 0.05, b.maxX - 0.05, b.minY + 0.01, b.maxZ - 0.05);
            if (!level().noCollision(this, slab)) return true;
        }
        return false;
    }

    private static double absMin(double a, double b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }

    private void clientTick() {
        double vx = getX() - xo, vy = getY() - yo, vz = getZ() - zo;
        clientSpeed = Math.sqrt(vx * vx + vy * vy + vz * vz);

        podAnimO = podAnim;
        podAnim = approach(podAnim, isPodsOut() ? 1f : 0f, 0.035f);
        shieldAnimO = shieldAnim;
        shieldAnim = approach(shieldAnim, isShielded() ? 1f : 0f, 0.06f);
        cloakAnimO = cloakAnim;
        cloakAnim = approach(cloakAnim, isCloaked() ? 1f : 0f, 0.05f);
        if (shieldFlash > 0) shieldFlash--;

        // Düsen: gelandet aus (mit Pilot nur Standglühen), schwebend an, mit Tempo heller
        engineGlowO = engineGlow;
        float glowTarget = !hasEnergy() ? 0f
                : onGround() ? (isVehicle() ? 0.25f : 0f) : 0.6f + Mth.clamp((float) clientSpeed * 0.5f, 0f, 0.4f);
        engineGlow += (glowTarget - engineGlow) * 0.08f;

        Vec3 l = left();
        double leftSpeed = l.x * vx + l.z * vz;
        float yawDelta = Mth.wrapDegrees(getYRot() - yRotO);
        float targetRoll = Mth.clamp(yawDelta * 5f - (float) leftSpeed * 28f, -30f, 30f);
        Vec3 f = forward();
        double fwdSpeed = f.x * vx + f.z * vz;
        float targetPitch = Mth.clamp((float) (-vy * 28 + fwdSpeed * 5), -28f, 28f);
        visRollO = visRoll;
        visPitchO = visPitch;
        visRoll += (targetRoll - visRoll) * 0.12f;
        visPitch += (targetPitch - visPitch) * 0.12f;

        // Ionen-Partikel an den Düsen
        if (!isCloaked() && engineGlow > 0.2f) {
            float pod = POD_X + 1.6f * podAnim;
            Vec3 back = f.scale(-0.04 - clientSpeed * 0.15);
            for (int side = -1; side <= 1; side += 2) {
                for (float ox : new float[]{-0.38f, 0.38f}) {
                    if (random.nextFloat() > engineGlow * 0.45f) continue;
                    Vec3 p = modelToWorld(side * pod + ox, POD_Y - 0.2 * podAnim + (random.nextFloat() - 0.5f) * 1.4f, POD_REAR_Z - 0.4);
                    level().addParticle(ION, p.x, p.y, p.z, back.x + vx * 0.3, back.y + vy * 0.3, back.z + vz * 0.3);
                    if (random.nextFloat() < 0.06f * engineGlow) {
                        level().addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, back.x * 4, (random.nextFloat() - 0.5f) * 0.1f, back.z * 4);
                    }
                }
            }
        }
        clientTicker.accept(this);
    }

    private void serverTick() {
        if (podsTimer > 0) podsTimer--;
        boolean landed = onGround();
        if (landed != lastLanded) {           // Landung/Start: Handsteuerung zurücksetzen
            podsOverride = null;
            lastLanded = landed;
        }
        boolean pods = podsTimer > 0 || (podsOverride != null ? podsOverride : !landed);
        if (pods != isPodsOut()) entityData.set(DATA_PODS_OUT, pods);

        Vec3 pos = position();
        serverVelocity = pos.subtract(lastServerPos);
        if (serverVelocity.lengthSqr() > 9) serverVelocity = Vec3.ZERO; // Teleport o.ä.
        lastServerPos = pos;

        for (Entity e : getPassengers()) e.resetFallDistance();
        if (underFireTimer > 0) underFireTimer--;
        if (impactCooldown > 0) impactCooldown--;
        tickEnergy();
        updateLights();

        // Wassereinbruch: ohne Schild 1 Hüllenpunkt pro Sekunde
        if (tickCount % 20 == 0 && isSubmerged() && !isShielded()) {
            applyHullDamage(WATER_DAMAGE, null, false);
            if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
                Vec3 p = position().add(0, HEIGHT * 0.7, 0);
                sl.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 12, 1.2 * RENDER_SIZE, 0.4 * RENDER_SIZE, 1.2 * RENDER_SIZE, 0.05);
            }
        }
    }

    private static float approach(float cur, float target, float step) {
        return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
    }

    // ------------------------------------------------------------------ Interpolation (wie Boot)

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            double x = getX() + (lerpX - getX()) / lerpSteps;
            double y = getY() + (lerpY - getY()) / lerpSteps;
            double z = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
            lerpSteps--;
            setPos(x, y, z);
            setRot(getYRot(), getXRot());
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 3;
    }

    // ------------------------------------------------------------------ Passagier

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        if (!hasPassenger(passenger)) return;
        // Pilotensitz vorne im Cockpit (Modell z ~ 6.3). Der Pilot dreht nicht mit dem Schiff (A/D lässt die Kamera stehen).
        Vec3 f = forward().scale(6.3 * MODEL_SCALE);
        move.accept(passenger, getX() + f.x, getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(), getZ() + f.z);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 f = forward(), l = left();
        Vec3 pos = position();
        Vec3[] candidates = {pos.add(f.scale(-5.0 * RENDER_SIZE)), pos.add(l.scale(2.4 * RENDER_SIZE)), pos.add(l.scale(-2.4 * RENDER_SIZE)), pos.add(f.scale(5.0 * RENDER_SIZE))};
        for (Vec3 cand : candidates) {
            BlockPos base = BlockPos.containing(cand);
            for (BlockPos bp : new BlockPos[]{base, base.above(), base.below()}) {
                double floor = level().getBlockFloorHeight(bp);
                if (!DismountHelper.isBlockFloorValid(floor)) continue;
                Vec3 v = Vec3.upFromBottomCenterOf(bp, floor);
                for (Pose pose : passenger.getDismountPoses()) {
                    if (DismountHelper.canDismountTo(level(), passenger, passenger.getLocalBoundsForPose(pose).move(v))) {
                        passenger.setPose(pose);
                        return v;
                    }
                }
            }
        }
        // In der Luft: seitlich aussteigen
        Vec3 side = pos.add(l.scale(2.4 * RENDER_SIZE));
        if (level().noCollision(passenger, passenger.getBoundingBox().move(side.subtract(passenger.position())))) {
            return side;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ Interaktion

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) openCustomInventoryScreen(player);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isVehicle()) return InteractionResult.PASS;
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (level().isClientSide) return;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.sixRows(id, inv, cargo),
                Component.translatable("container.puddlejumper.cargo")));
        gameEvent(GameEvent.CONTAINER_OPEN, player);
    }

    public SimpleContainer getCargo() {
        return cargo;
    }

    /** Server: Aktion des Piloten ausführen. */
    public void handleAction(ServerPlayer player, JumperActionPacket.Action action, int targetId) {
        if (getControllingPassenger() != player) return;
        boolean turningOn = switch (action) {
            case CLOAK -> !isCloaked();
            case SHIELD -> !isShielded();
            case LIGHTS -> !isLightsOn();
            default -> false;
        };
        if (turningOn && !hasEnergy()) {
            denied();
            return;
        }
        switch (action) {
            case CLOAK -> toggleCloak();
            case SHIELD -> {
                setShielded(!isShielded());
                playSound(isShielded() ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE, 1.0f, isShielded() ? 1.4f : 1.8f);
            }
            case PODS -> {
                podsOverride = !isPodsOut();
                podsTimer = 0;
                playSound(podsOverride ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 0.8f, 0.7f);
            }
            case FIRE -> fireDrone(player, targetId);
            case LIGHTS -> {
                setLightsOn(!isLightsOn());
                playSound(SoundEvents.LEVER_CLICK, 0.6f, isLightsOn() ? 1.2f : 0.9f);
            }
        }
    }

    // ------------------------------------------------------------------ Energie

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) initEnergy();
    }

    private void initEnergy() {
        int cap = Config.CAPACITY.get();
        if (entityData.get(DATA_CAPACITY) != cap) {
            entityData.set(DATA_CAPACITY, cap);
            if (getEnergy() > cap) entityData.set(DATA_ENERGY, cap);
        }
        if (!energyInitialized) setEnergy((int) Math.round(cap * Config.START_CHARGE.get()));
    }

    /** Server: Verbrauch aller Systeme abbuchen, gelandet aus Ladestationen nachladen. */
    private void tickEnergy() {
        boolean water = isSubmerged();
        double underFire = underFireTimer > 0 ? Config.UNDER_FIRE_FACTOR.get() : 1.0;
        double drain = 0;
        if (!onGround() && hasEnergy()) {   // Antrieb nur in der Luft; gelandet kostet er nichts
            double speed = Mth.clamp(serverVelocity.length() / MAX_SPEED, 0, 1);
            double drive = Config.HOVER_DRAIN.get() + (Config.THRUST_DRAIN.get() - Config.HOVER_DRAIN.get()) * speed;
            drain += water ? drive * Config.WATER_DRIVE_FACTOR.get() : drive;
        }
        if (isShielded()) drain += Config.SHIELD_DRAIN.get() * (water ? Config.SHIELD_WATER_FACTOR.get() : 1) * underFire;
        if (isCloaked()) drain += Config.CLOAK_DRAIN.get() * (water ? Config.CLOAK_WATER_FACTOR.get() : 1) * underFire;
        if (isLightsOn() && !isCloaked()) drain += Config.LIGHT_DRAIN.get();
        int cost = (int) Math.round(drain);
        if (cost > 0 && !consume(cost)) powerFailure();

        if (onGround() && tickCount % CHARGE_INTERVAL == 0) lastChargePerTick = chargeFromPads() / CHARGE_INTERVAL;
        else if (!onGround()) lastChargePerTick = 0;

        flowAvg += ((cost - lastChargePerTick) - flowAvg) * 0.1f;
        entityData.set(DATA_FLOW, Math.round(flowAvg));
    }

    /** Energie abbuchen; false (und Speicher leer), wenn sie nicht reicht. */
    private boolean consume(int amount) {
        int e = getEnergy();
        if (e >= amount) {
            setEnergy(e - amount);
            return true;
        }
        setEnergy(0);
        return false;
    }

    /** Speicher leer: alles schaltet ab, der Jumper stürzt ab. */
    private void powerFailure() {
        if (isShielded() || isCloaked() || isLightsOn()) {
            playSound(SoundEvents.BEACON_DEACTIVATE, 1.2f, 0.6f);
        }
        playSound(SoundEvents.CONDUIT_DEACTIVATE, 1.5f, 0.7f);
        setShielded(false);
        setCloaked(false);
        setLightsOn(false);
    }

    /** Aus allen Ladestationen direkt unter Bug, Mitte und Heck laden. @return geladene FE */
    private int chargeFromPads() {
        int room = getCapacity() - getEnergy();
        Set<BlockPos> below = new HashSet<>();
        int y = Mth.floor(getY() - 0.05);
        for (double off : new double[]{SEGMENT_OFFSET, 0, -SEGMENT_OFFSET}) {
            AABB b = segmentBox(off);
            for (int x = Mth.floor(b.minX + 0.05); x <= Mth.floor(b.maxX - 0.05); x++) {
                for (int z = Mth.floor(b.minZ + 0.05); z <= Mth.floor(b.maxZ - 0.05); z++) {
                    below.add(new BlockPos(x, y, z));
                }
            }
        }
        float fill = getEnergyFraction();
        int gained = 0;
        for (BlockPos p : below) {
            if (level().getBlockEntity(p) instanceof ChargingPadBlockEntity pad) {
                gained += pad.drawForJumper(room - gained, CHARGE_INTERVAL, fill);
            }
        }
        if (gained > 0) setEnergy(getEnergy() + gained);
        return gained;
    }

    /**
     * Schild fängt den Treffer ab, wenn genug Energie da ist (kostet pro Schadenspunkt).
     * Reicht sie nicht, bricht der Schild zusammen und der Treffer geht durch.
     */
    private boolean shieldAbsorbs(float amount) {
        underFireTimer = UNDER_FIRE_TICKS;
        if (!isShielded()) return false;
        int cost = Math.round(amount * Config.SHIELD_HIT_COST.get());
        if (consume(cost)) {
            level().broadcastEntityEvent(this, EVENT_SHIELD_HIT);
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.6f);
            return true;
        }
        setShielded(false);
        playSound(SoundEvents.BEACON_DEACTIVATE, 1.2f, 0.8f);
        if (!hasEnergy()) powerFailure();
        return false;
    }

    private void denied() {
        playSound(SoundEvents.DISPENSER_FAIL, 0.8f, 1.2f);
    }

    // ------------------------------------------------------------------ Licht

    /** Licht-Stufe der Scheinwerfer (wie eine Fackel = 14, hier volle 15). */
    private static final int LIGHT_LEVEL = 15;
    /** Ein Licht wird erst versetzt, wenn das Schiff so weit (Blöcke) davon weg ist ... */
    private static final double LIGHT_MOVE_DIST = 2.0;
    /** ... und höchstens alle so viele Ticks - spart Licht-Neuberechnung und Chunk-Neubau beim Client. */
    private static final int LIGHT_INTERVAL = 4;
    /** Ab diesem Tempo (Blöcke/Tick) nur noch das Bug-Licht. */
    private static final double LIGHT_FAST_SPEED = 0.5;
    private static final int LIGHT_CENTER = 0, LIGHT_BOW = 1;
    /** Vom Jumper gesetzte unsichtbare Lichtblöcke je Platz (Mitte, Bug), null = keiner. */
    private final BlockPos[] lightSlots = new BlockPos[2];
    /** Aus dem Spielstand geladene Lichtblöcke (z. B. nach Absturz) - werden beim ersten Tick entfernt. */
    private final List<BlockPos> staleLights = new ArrayList<>();

    /**
     * Minecraft kennt keine bewegten Lichtquellen: Der Server setzt unsichtbare Vanilla-Lichtblöcke
     * (minecraft:light) in Rumpfmitte und vor den Bug und räumt sie hinter dem Schiff wieder weg.
     * Nur in Luft oder Wasser (dann wassergeflutet). Getarnt bleibt es dunkel.
     *
     * <p>Sparsam: ein Licht wandert erst mit, wenn das Schiff mehr als {@link #LIGHT_MOVE_DIST} Blöcke
     * davon entfernt ist, und höchstens alle {@link #LIGHT_INTERVAL} Ticks; bei hohem Tempo nur das Bug-Licht.</p>
     */
    private void updateLights() {
        if (!staleLights.isEmpty()) {
            for (BlockPos p : staleLights) removeLight(p);
            staleLights.clear();
        }
        if (!isLightsOn() || isCloaked() || !isAlive()) {
            clearLights();
            return;
        }
        boolean fast = serverVelocity.length() > LIGHT_FAST_SPEED;
        boolean moveTick = tickCount % LIGHT_INTERVAL == 0;
        Vec3 center = position().add(0, HEIGHT * 0.5, 0);
        Vec3[] targets = {center, center.add(forward().scale(5.0 * RENDER_SIZE))};
        for (int i = 0; i < lightSlots.length; i++) {
            BlockPos cur = lightSlots[i];
            if (i == LIGHT_CENTER && fast) {               // schnell: Mitte aus
                if (cur != null) removeLight(cur);
                lightSlots[i] = null;
                continue;
            }
            // neues Licht sofort, bestehendes nur im Takt und bei genügend Abstand versetzen
            if (cur != null && (!moveTick || cur.getCenter().distanceTo(targets[i]) <= LIGHT_MOVE_DIST)) continue;
            BlockPos next = BlockPos.containing(targets[i]);
            if (next.equals(cur) || (cur != null && next.equals(lightSlots[1 - i]))) continue;
            if (placeLight(next)) {
                if (cur != null) removeLight(cur);
                lightSlots[i] = next;
            } else if (cur != null && cur.getCenter().distanceTo(targets[i]) > 2 * LIGHT_MOVE_DIST) {
                removeLight(cur);                          // kein Platz (Block im Weg): altes Licht nicht hinterherziehen
                lightSlots[i] = null;
            }
        }
    }

    private boolean placeLight(BlockPos p) {
        if (!level().isLoaded(p)) return false;
        BlockState old = level().getBlockState(p);
        boolean water = old.is(Blocks.WATER) && old.getFluidState().isSource();
        if (!old.isAir() && !water) return false;
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL)
                .setValue(BlockStateProperties.WATERLOGGED, water);
        return level().setBlock(p, light, Block.UPDATE_CLIENTS);
    }

    private void removeLight(BlockPos p) {
        if (!level().isLoaded(p)) return;
        BlockState s = level().getBlockState(p);
        if (!s.is(Blocks.LIGHT)) return; // inzwischen etwas anderes dort
        BlockState back = s.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        level().setBlock(p, back, Block.UPDATE_CLIENTS);
    }

    private void clearLights() {
        for (int i = 0; i < lightSlots.length; i++) {
            if (lightSlots[i] != null) removeLight(lightSlots[i]);
            lightSlots[i] = null;
        }
    }

    /** Aktuell gesetzte Lichtblöcke (für Tests). */
    public List<BlockPos> getLightBlocks() {
        List<BlockPos> list = new ArrayList<>(2);
        for (BlockPos p : lightSlots) if (p != null) list.add(p);
        return list;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) clearLights();
        super.remove(reason);
    }

    private void toggleCloak() {
        boolean cloak = !isCloaked();
        setCloaked(cloak);
        playSound(cloak ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.6f);
        if (cloak) {
            // Mobs verlieren den Piloten aus den Augen
            for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(48))) {
                if (mob.getTarget() != null && hasPassenger(mob.getTarget())) mob.setTarget(null);
            }
        }
    }

    public boolean droneReady() {
        return level().getGameTime() - lastDroneTick >= DRONE_COOLDOWN;
    }

    private void fireDrone(ServerPlayer pilot, int targetId) {
        if (!droneReady()) return;
        lastDroneTick = level().getGameTime();
        if (getEnergy() < Config.DRONE_COST.get()) {
            denied();
            return;
        }
        consume(Config.DRONE_COST.get());
        if (isCloaked()) toggleCloak(); // Waffen nur ungetarnt
        podsTimer = 100;
        entityData.set(DATA_PODS_OUT, true);

        Entity target = targetId >= 0 ? level().getEntity(targetId) : null;
        if (!isValidTarget(target) || target.distanceToSqr(this) > 96 * 96) {
            target = findAutoTarget();
        }

        leftLauncher = !leftLauncher;
        int side = leftLauncher ? 1 : -1;
        Vec3 spawn = modelToWorld(side * 4.2, 4.3, -1.0);
        float yaw = pilot.getYRot() * Mth.DEG_TO_RAD;
        float pitch = pilot.getXRot() * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        Vec3 vel = look.scale(0.45).add(left().scale(side * 0.12)).add(0, 0.08, 0).add(serverVelocity);

        DroneEntity drone = new DroneEntity(level(), pilot, this, target);
        drone.setPos(spawn.x, spawn.y, spawn.z);
        drone.setDeltaMovement(vel);
        level().addFreshEntity(drone);
        level().playSound(null, spawn.x, spawn.y, spawn.z, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.8f, 1.8f);
        level().playSound(null, spawn.x, spawn.y, spawn.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 0.6f);
    }

    @Nullable
    private LivingEntity findAutoTarget() {
        Vec3 f = forward();
        Vec3 c = position();
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(40), e ->
                        e instanceof Enemy && e.isAlive() && !hasPassenger(e)
                                && e.position().subtract(c).normalize().dot(f) > 0.3)
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
    }

    // ------------------------------------------------------------------ Schaden

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (level().isClientSide || isRemoved()) return true;
        Entity attacker = source.getEntity();
        if (attacker != null && hasPassenger(attacker)) return false;
        if (source.getDirectEntity() instanceof DroneEntity drone && drone.isFriendly(this)) return false;

        // Drohnen: fester Anteil der Hülle; Treffer + Explosion derselben Drohne zählen nur einmal
        if (source.getDirectEntity() instanceof DroneEntity drone) {
            if (drone.getUUID().equals(lastDroneHit)) return false;
            lastDroneHit = drone.getUUID();
            float full = MAX_HEALTH * DRONE_HIT_FRACTION;
            amount = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) ? Math.min(full, amount * full / 25f) : full;
        }
        // Schild: hält alles ab, solange die Energie reicht; Treffer lässt die Blase kurz aufleuchten
        if (shieldAbsorbs(amount)) return false;

        applyHullDamage(amount, attacker, true);
        return true;
    }

    /** Hülle beschädigen; bei 0 zerstören (Item + Ladung fallen heraus, Kreativ: nur Ladung). */
    private void applyHullDamage(float amount, @Nullable Entity attacker, boolean feedback) {
        if (feedback) {
            setHurtDir(-getHurtDir());
            setHurtTime(10);
            setDamage(Math.min(200f, getDamage() + amount * 10f));
            playSound(SoundEvents.ANVIL_LAND, 0.25f, 1.6f);
        }
        setHealth(getHealth() - amount);
        gameEvent(GameEvent.ENTITY_DAMAGE, attacker);

        boolean creative = attacker instanceof Player p && p.getAbilities().instabuild;
        if (getHealth() <= 0f) {
            if (!creative && level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) {
                spawnAtLocation(ModRegistry.PUDDLE_JUMPER_ITEM.get());
            }
            Containers.dropContents(level(), this, cargo);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SHIELD_HIT) {
            shieldFlash = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void animateHurt(float yaw) {
        setHurtDir(-getHurtDir());
        setHurtTime(10);
        setDamage(getDamage() * 11f);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Kein Rückstoß: weder Spieler/Mobs noch Treffer können den Jumper wegschieben. */
    @Override
    public void push(double x, double y, double z) {}

    @Override
    public void push(Entity other) {}

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** Fest wie ein Boot - auch getarnt. Man kann nicht hindurchlaufen und darauf stehen. */
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        if (other instanceof PuddleJumperPart part && part.getParent() == this) return false;
        return super.canCollideWith(other);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModRegistry.PUDDLE_JUMPER_ITEM.get());
    }

    @Override
    protected float getEyeHeight(Pose pose, net.minecraft.world.entity.EntityDimensions dims) {
        return dims.height * 0.6f;
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setCloaked(tag.getBoolean("Cloaked"));
        setShielded(tag.getBoolean("Shielded"));
        setLightsOn(tag.getBoolean("Lights"));
        // nach Absturz/Neustart liegengebliebene Lichtblöcke beim nächsten Tick aufräumen
        staleLights.clear();
        for (long l : tag.getLongArray("LightBlocks")) staleLights.add(BlockPos.of(l));
        setHealth(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
        if (tag.contains("Energy")) {   // ältere Jumper ohne Energie bekommen beim ersten Tick die Startladung
            entityData.set(DATA_CAPACITY, Math.max(1, tag.getInt("Capacity")));
            setEnergy(tag.getInt("Energy"));
        }
        cargo.clearContent();
        ListTag list = tag.getList("Cargo", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            int slot = c.getByte("Slot") & 255;
            if (slot < cargo.getContainerSize()) cargo.setItem(slot, ItemStack.of(c));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Cloaked", isCloaked());
        tag.putBoolean("Shielded", isShielded());
        tag.putBoolean("Lights", isLightsOn());
        List<BlockPos> lights = new ArrayList<>(staleLights);
        lights.addAll(getLightBlocks());
        tag.putLongArray("LightBlocks", lights.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putFloat("Health", getHealth());
        if (energyInitialized) {
            tag.putInt("Energy", getEnergy());
            tag.putInt("Capacity", getCapacity());
        }
        ListTag list = new ListTag();
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack stack = cargo.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag c = new CompoundTag();
                c.putByte("Slot", (byte) i);
                stack.save(c);
                list.add(c);
            }
        }
        tag.put("Cargo", list);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!isSilent()) level().playSound(null, getX(), getY(), getZ(), sound, getSoundSource(), volume, pitch);
    }

    /** Laderaum (54 Plätze). */
    private class CargoContainer extends SimpleContainer {
        CargoContainer() {
            super(54);
        }

        @Override
        public boolean stillValid(Player player) {
            return isAlive() && player.distanceToSqr(PuddleJumperEntity.this) < 12 * 12;
        }
    }
}
