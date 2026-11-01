package de.matthias.puddlejumper.entity;

import de.matthias.puddlejumper.ModRegistry;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Der Puddle Jumper: ein steuerbares Fluggerät mit Tarnung, Heckklappe samt Laderaum und Drohnenwerfern.
 *
 * <p>Wie bei Booten simuliert der Client des Piloten die Bewegung und schickt die Position an den Server.
 * Ohne Pilot simuliert der Server (Schwerkraft, sanftes Absinken).</p>
 */
public class PuddleJumperEntity extends Entity implements HasCustomInventoryScreen {
    public static final float WIDTH = 2.6f;
    public static final float HEIGHT = 2.0f;
    /** Modell-Einheiten (OBJ) -> Blöcke. Das Modell ist 28.9 Einheiten lang -> 8 Blöcke. */
    public static final float MODEL_SCALE = 8.0f / 28.9f;
    /** Abstand der Bug-/Heck-Hitbox vom Mittelpunkt (Blöcke). */
    private static final double SEGMENT_OFFSET = 2.7;

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
    /** Selbstreparatur: nach 30 s ohne Schaden 1 Punkt pro Sekunde. */
    private static final int REPAIR_DELAY = 600, REPAIR_INTERVAL = 20;

    private static final EntityDataAccessor<Boolean> DATA_CLOAKED = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHIELD = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PODS_OUT = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_HURT_TIME = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HURT_DIR = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_HEALTH = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.FLOAT);

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
    private int ticksSinceDamage = REPAIR_DELAY;
    @Nullable private java.util.UUID lastDroneHit;
    /** Drehung, die der Pilot noch mitmachen muss (wird in positionRider angewendet -> ruckelfrei). */
    private float riderYawDelta;
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
        entityData.define(DATA_HURT_TIME, 0);
        entityData.define(DATA_HURT_DIR, 1);
        entityData.define(DATA_DAMAGE, 0f);
        entityData.define(DATA_HEALTH, MAX_HEALTH);
    }

    public boolean isCloaked() { return entityData.get(DATA_CLOAKED); }
    public void setCloaked(boolean v) { entityData.set(DATA_CLOAKED, v); }
    public boolean isShielded() { return entityData.get(DATA_SHIELD); }
    public void setShielded(boolean v) { entityData.set(DATA_SHIELD, v); }
    public boolean isPodsOut() { return entityData.get(DATA_PODS_OUT); }

    /** Triebwerke laufen: in der Luft (auch ohne Pilot) oder mit Pilot an Bord. */
    public boolean enginesRunning() {
        return isVehicle() || !onGround();
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
     * Leertaste/Strg steigen/sinken. Die Kamera des Piloten dreht beim Kurven mit (wie im Boot).
     */
    private void flyControlled(Player pilot) {
        FlightInput in = FlightInput.source.get();

        yawVelocity = yawVelocity * TURN_DRAG - in.turn() * TURN_ACCEL; // A (turn > 0) = links
        if (in.turn() == 0 && Math.abs(yawVelocity) < 0.02f) yawVelocity = 0;
        setYRot(getYRot() + yawVelocity);
        riderYawDelta += yawVelocity;

        double fwdAccel = in.forward() > 0 ? ACCEL : ACCEL * 0.6;
        Vec3 acc = forward().scale(in.forward() * fwdAccel).add(0, in.vertical() * ACCEL * 0.8, 0);

        boolean idle = in.forward() == 0 && in.vertical() == 0;
        Vec3 v = getDeltaMovement().scale(idle ? DRAG_IDLE : DRAG_THRUST).add(acc);
        if (v.lengthSqr() > MAX_SPEED * MAX_SPEED) v = v.normalize().scale(MAX_SPEED);
        if (idle && v.lengthSqr() < 1.0e-5) v = Vec3.ZERO;
        setDeltaMovement(v);
    }

    /** Ohne Pilot: Schwebe-Modus. Der Jumper rollt aus und bleibt in der Luft stehen. */
    private void idlePhysics() {
        Vec3 v = getDeltaMovement().scale(onGround() ? 0.6 : DRAG_IDLE);
        if (isInWater()) v = v.add(0, 0.03, 0);
        setDeltaMovement(v.lengthSqr() < 1.0e-5 ? Vec3.ZERO : v);
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
        move(MoverType.SELF, d);
        Vec3 v = getDeltaMovement();
        setDeltaMovement(d.x != delta.x ? 0 : v.x, d.y != delta.y ? 0 : v.y, d.z != delta.z ? 0 : v.z);
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
        float glowTarget = onGround() ? (isVehicle() ? 0.25f : 0f) : 0.6f + Mth.clamp((float) clientSpeed * 0.5f, 0f, 0.4f);
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

        // Antiker-Technik: langsame Selbstreparatur, wenn eine Weile kein Schaden kam
        if (++ticksSinceDamage >= REPAIR_DELAY && getHealth() < MAX_HEALTH && tickCount % REPAIR_INTERVAL == 0) {
            setHealth(getHealth() + 1f);
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
        // Pilotensitz vorne im Cockpit (Modell z ~ 6.3)
        // Pilot dreht mit dem Schiff - hier (nach dem Reset der alten Blickrichtung) statt im Schiffs-Tick,
        // sonst springt die Kamera jeden Tick ohne Interpolation
        if (riderYawDelta != 0 && passenger == getControllingPassenger()) {
            passenger.setYRot(passenger.getYRot() + riderYawDelta);
            passenger.setYHeadRot(passenger.getYHeadRot() + riderYawDelta);
            riderYawDelta = 0;
        }
        Vec3 f = forward().scale(6.3 * MODEL_SCALE);
        move.accept(passenger, getX() + f.x, getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(), getZ() + f.z);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 f = forward(), l = left();
        Vec3 pos = position();
        Vec3[] candidates = {pos.add(f.scale(-5.0)), pos.add(l.scale(2.4)), pos.add(l.scale(-2.4)), pos.add(f.scale(5.0))};
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
        Vec3 side = pos.add(l.scale(2.4));
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
        }
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
        if (isShielded()) {
            // Schild: unzerstörbar, Treffer lässt die Blase kurz aufleuchten
            level().broadcastEntityEvent(this, EVENT_SHIELD_HIT);
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.6f);
            return false;
        }

        // Drohnen: fester Anteil der Hülle; Treffer + Explosion derselben Drohne zählen nur einmal
        if (source.getDirectEntity() instanceof DroneEntity drone) {
            if (drone.getUUID().equals(lastDroneHit)) return false;
            lastDroneHit = drone.getUUID();
            float full = MAX_HEALTH * DRONE_HIT_FRACTION;
            amount = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) ? Math.min(full, amount * full / 25f) : full;
        }

        setHurtDir(-getHurtDir());
        setHurtTime(10);
        setDamage(Math.min(200f, getDamage() + amount * 10f));
        setHealth(getHealth() - amount);
        ticksSinceDamage = 0;
        gameEvent(GameEvent.ENTITY_DAMAGE, attacker);
        playSound(SoundEvents.ANVIL_LAND, 0.25f, 1.6f);

        // Kreativ: gleiche Haltbarkeit, beim Zerstören fällt aber kein Item heraus (wie bei Booten)
        boolean creative = attacker instanceof Player p && p.getAbilities().instabuild;
        if (getHealth() <= 0f) {
            if (!creative && level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) {
                spawnAtLocation(ModRegistry.PUDDLE_JUMPER_ITEM.get());
            }
            Containers.dropContents(level(), this, cargo);
            discard();
        }
        return true;
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
        setHealth(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
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
        tag.putFloat("Health", getHealth());
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
