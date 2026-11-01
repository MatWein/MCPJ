package de.matthias.puddlejumper.entity;

import de.matthias.puddlejumper.ModRegistry;
import de.matthias.puddlejumper.network.JumperActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
    private static final double ACCEL = 0.06;
    private static final double MAX_SPEED = 1.3;
    private static final float MAX_TURN = 6.0f;

    public static final int DRONE_COOLDOWN = 12;
    private static final float BREAK_DAMAGE = 600f;

    private static final EntityDataAccessor<Boolean> DATA_CLOAKED = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HATCH_OPEN = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PODS_OUT = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_HURT_TIME = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HURT_DIR = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(PuddleJumperEntity.class, EntityDataSerializers.FLOAT);

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
    private boolean podsManual;
    private int podsTimer;
    private long lastDroneTick = -100;
    private boolean leftLauncher;
    private Vec3 lastServerPos = Vec3.ZERO;
    private Vec3 serverVelocity = Vec3.ZERO;

    // Client-Animation
    public float hatchAnim, hatchAnimO;
    public float podAnim, podAnimO;
    public float visRoll, visRollO;
    public float visPitch, visPitchO;
    public double clientSpeed;

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
        entityData.define(DATA_HATCH_OPEN, false);
        entityData.define(DATA_PODS_OUT, false);
        entityData.define(DATA_HURT_TIME, 0);
        entityData.define(DATA_HURT_DIR, 1);
        entityData.define(DATA_DAMAGE, 0f);
    }

    public boolean isCloaked() { return entityData.get(DATA_CLOAKED); }
    public void setCloaked(boolean v) { entityData.set(DATA_CLOAKED, v); }
    public boolean isHatchOpen() { return entityData.get(DATA_HATCH_OPEN); }
    public void setHatchOpen(boolean v) { entityData.set(DATA_HATCH_OPEN, v); }
    public boolean isPodsOut() { return entityData.get(DATA_PODS_OUT); }
    public int getHurtTime() { return entityData.get(DATA_HURT_TIME); }
    public void setHurtTime(int v) { entityData.set(DATA_HURT_TIME, v); }
    public int getHurtDir() { return entityData.get(DATA_HURT_DIR); }
    public void setHurtDir(int v) { entityData.set(DATA_HURT_DIR, v); }
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

        if (isControlledByLocalInstance()) {
            if (getControllingPassenger() instanceof Player pilot) {
                flyControlled(pilot);
            } else {
                idlePhysics();
            }
            moveWithSegments(getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }

        updateParts();

        if (level().isClientSide) {
            clientTick();
        } else {
            serverTick();
        }
    }

    private void flyControlled(Player pilot) {
        FlightInput in = FlightInput.source.get();

        // Bug folgt sanft der Blickrichtung des Piloten
        float diff = Mth.wrapDegrees(pilot.getYRot() - getYRot());
        setYRot(getYRot() + Mth.clamp(diff * 0.25f, -MAX_TURN, MAX_TURN));

        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float pitch = pilot.getXRot() * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        Vec3 left = left();

        double fwdAccel = in.forward() > 0 ? ACCEL : ACCEL * 0.6;
        Vec3 acc = look.scale(in.forward() * fwdAccel)
                .add(left.scale(in.strafe() * ACCEL * 0.6))
                .add(0, in.vertical() * ACCEL * 0.8, 0);

        double drag = in.idle() ? 0.90 : 0.95;
        Vec3 v = getDeltaMovement().scale(drag).add(acc);
        if (v.lengthSqr() > MAX_SPEED * MAX_SPEED) v = v.normalize().scale(MAX_SPEED);
        if (in.idle() && v.lengthSqr() < 1.0e-5) v = Vec3.ZERO;
        setDeltaMovement(v);
    }

    private void idlePhysics() {
        Vec3 v = getDeltaMovement();
        double h = onGround() ? 0.5 : 0.9;
        double vy = (v.y - 0.04) * 0.98;
        if (isInWater()) vy += 0.07;
        setDeltaMovement(v.x * h, Math.max(vy, -1.5), v.z * h);
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
        // Haben Bug/Heck die Abwärtsbewegung gestoppt, merkt move() das nicht selbst
        if (delta.y < 0 && d.y > delta.y) {
            verticalCollision = true;
            verticalCollisionBelow = true;
            setOnGround(true);
        }
        Vec3 v = getDeltaMovement();
        setDeltaMovement(d.x != delta.x ? 0 : v.x, d.y != delta.y ? 0 : v.y, d.z != delta.z ? 0 : v.z);
    }

    private static double absMin(double a, double b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }

    private void clientTick() {
        hatchAnimO = hatchAnim;
        hatchAnim = approach(hatchAnim, isHatchOpen() ? 1f : 0f, 0.035f);

        double vx = getX() - xo, vy = getY() - yo, vz = getZ() - zo;
        clientSpeed = Math.sqrt(vx * vx + vy * vy + vz * vz);

        podAnimO = podAnim;
        boolean wantPods = isPodsOut() || clientSpeed > 0.75;
        podAnim = approach(podAnim, wantPods ? 1f : 0f, 0.04f);

        Vec3 l = left();
        double leftSpeed = l.x * vx + l.z * vz;
        float yawDelta = Mth.wrapDegrees(getYRot() - yRotO);
        float targetRoll = Mth.clamp(yawDelta * 3.5f - (float) leftSpeed * 28f, -32f, 32f);
        Vec3 f = forward();
        double fwdSpeed = f.x * vx + f.z * vz;
        float targetPitch = Mth.clamp((float) (-vy * 28 + fwdSpeed * 5), -28f, 28f);
        visRollO = visRoll;
        visPitchO = visPitch;
        visRoll += (targetRoll - visRoll) * 0.15f;
        visPitch += (targetPitch - visPitch) * 0.12f;

        // Triebwerksglühen/-partikel
        if (isVehicle() && !isCloaked() && random.nextFloat() < 0.3f + clientSpeed) {
            float pod = 4.85f + 1.6f * podAnim;
            for (int side = -1; side <= 1; side += 2) {
                Vec3 p = modelToWorld(side * pod, 4.3 - 0.2 * podAnim, -12.2);
                level().addParticle(new DustParticleOptions(new Vector3f(0.55f, 0.85f, 1.0f), 0.9f),
                        p.x, p.y, p.z, -vx * 0.2, -vy * 0.2, -vz * 0.2);
            }
        }
        clientTicker.accept(this);
    }

    private void serverTick() {
        if (podsTimer > 0) podsTimer--;
        boolean pods = podsManual || podsTimer > 0;
        if (pods != isPodsOut()) entityData.set(DATA_PODS_OUT, pods);

        Vec3 pos = position();
        serverVelocity = pos.subtract(lastServerPos);
        if (serverVelocity.lengthSqr() > 9) serverVelocity = Vec3.ZERO; // Teleport o.ä.
        lastServerPos = pos;

        for (Entity e : getPassengers()) e.resetFallDistance();
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
            case HATCH -> {
                setHatchOpen(!isHatchOpen());
                cargo.autoOpened = false;
                playSound(isHatchOpen() ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, 1.0f, 0.6f);
            }
            case PODS -> {
                podsManual = !podsManual;
                playSound(SoundEvents.IRON_DOOR_OPEN, 0.8f, 0.7f);
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
        if (!(target instanceof LivingEntity le) || !le.isAlive() || target.distanceToSqr(this) > 96 * 96 || hasPassenger(target)) {
            target = findAutoTarget();
        }

        leftLauncher = !leftLauncher;
        int side = leftLauncher ? 1 : -1;
        Vec3 spawn = modelToWorld(side * 4.2, 4.3, -1.0);
        float yaw = pilot.getYRot() * Mth.DEG_TO_RAD;
        float pitch = pilot.getXRot() * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        Vec3 vel = look.scale(0.9).add(left().scale(side * 0.25)).add(0, 0.15, 0).add(serverVelocity);

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
        if (source.getDirectEntity() instanceof DroneEntity) return false;

        setHurtDir(-getHurtDir());
        setHurtTime(10);
        markHurt();
        setDamage(getDamage() + amount * 10f);
        gameEvent(GameEvent.ENTITY_DAMAGE, attacker);
        playSound(SoundEvents.ANVIL_LAND, 0.25f, 1.6f);

        boolean creative = attacker instanceof Player p && p.getAbilities().instabuild;
        if (creative || getDamage() > BREAK_DAMAGE) {
            if (!creative && level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) {
                spawnAtLocation(ModRegistry.PUDDLE_JUMPER_ITEM.get());
            }
            Containers.dropContents(level(), this, cargo);
            discard();
        }
        return true;
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
        setHatchOpen(tag.getBoolean("HatchOpen"));
        podsManual = tag.getBoolean("PodsOut");
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
        tag.putBoolean("HatchOpen", isHatchOpen() && !cargo.autoOpened);
        tag.putBoolean("PodsOut", podsManual);
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

    /** Laderaum: öffnet automatisch die Heckklappe, solange jemand von außen hineinschaut. */
    private class CargoContainer extends SimpleContainer {
        private int viewers;
        boolean autoOpened;

        CargoContainer() {
            super(54);
        }

        @Override
        public boolean stillValid(Player player) {
            return isAlive() && player.distanceToSqr(PuddleJumperEntity.this) < 12 * 12;
        }

        @Override
        public void startOpen(Player player) {
            viewers++;
            if (!level().isClientSide && !hasPassenger(player) && !isHatchOpen()) {
                setHatchOpen(true);
                autoOpened = true;
                PuddleJumperEntity.this.playSound(SoundEvents.PISTON_EXTEND, 1.0f, 0.6f);
            }
        }

        @Override
        public void stopOpen(Player player) {
            viewers = Math.max(0, viewers - 1);
            if (!level().isClientSide && viewers == 0 && autoOpened) {
                setHatchOpen(false);
                autoOpened = false;
                PuddleJumperEntity.this.playSound(SoundEvents.PISTON_CONTRACT, 1.0f, 0.6f);
            }
        }
    }
}
