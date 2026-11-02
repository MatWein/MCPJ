package de.matthias.puddlejumper.entity.jumper;

import de.matthias.puddlejumper.entity.drone.DroneEntity;
import de.matthias.puddlejumper.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class PuddleJumperEntity extends Entity implements HasCustomInventoryScreen {
    static final EntityDataAccessor<Boolean> CLOAKED = define(EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> SHIELDED = define(EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> LIGHTS_ON = define(EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> PODS_OUT = define(EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Integer> HURT_TIME = define(EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> HURT_DIRECTION = define(EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> HURT_WOBBLE = define(EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> HULL_INTEGRITY = define(EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> ENERGY = define(EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> CAPACITY = define(EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> ENERGY_FLOW = define(EntityDataSerializers.INT);

    static final byte SHIELD_HIT_EVENT = 60;
    private static final float EYE_HEIGHT_FACTOR = 0.6f;
    private static final double CULLING_MARGIN_HORIZONTAL = 4.5;
    private static final double CULLING_MARGIN_VERTICAL = 1.0;
    private static final double TELEPORT_DISTANCE_SQR = 9.0;
    private static final float MAX_STEP_HEIGHT = 1.0f;

    public static Consumer<PuddleJumperEntity> clientTicker = jumper -> {};

    private final PuddleJumperPart bow;
    private final PuddleJumperPart stern;
    private final PuddleJumperPart[] parts;

    private final JumperFlight flight = new JumperFlight(this);
    private final JumperInterpolation interpolation = new JumperInterpolation(this);
    private final JumperEnergy energy = new JumperEnergy(this);
    private final JumperHull hull = new JumperHull(this);
    private final JumperSystems systems = new JumperSystems(this);
    private final JumperPods pods = new JumperPods(this);
    private final JumperWeapons weapons = new JumperWeapons(this);
    private final JumperLights lights = new JumperLights(this);
    private final JumperCargo cargo = new JumperCargo(this);
    private final JumperAnimation animation = new JumperAnimation(this);

    private Vec3 lastServerPosition = Vec3.ZERO;
    private Vec3 serverVelocity = Vec3.ZERO;
    private int lavaCheckTick = -1;
    private boolean touchesLava;

    public PuddleJumperEntity(EntityType<? extends PuddleJumperEntity> type, Level level) {
        super(type, level);
        bow = new PuddleJumperPart(this, JumperGeometry.WIDTH, JumperGeometry.HEIGHT);
        stern = new PuddleJumperPart(this, JumperGeometry.WIDTH, JumperGeometry.HEIGHT);
        parts = new PuddleJumperPart[]{bow, stern};
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        setNoGravity(true);
        blocksBuilding = true;
        setMaxUpStep(MAX_STEP_HEIGHT);
    }

    private static <T> EntityDataAccessor<T> define(EntityDataSerializer<T> serializer) {
        return SynchedEntityData.defineId(PuddleJumperEntity.class, serializer);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(CLOAKED, false);
        entityData.define(SHIELDED, false);
        entityData.define(LIGHTS_ON, false);
        entityData.define(PODS_OUT, false);
        entityData.define(HURT_TIME, 0);
        entityData.define(HURT_DIRECTION, 1);
        entityData.define(HURT_WOBBLE, 0f);
        entityData.define(HULL_INTEGRITY, 1f);
        entityData.define(ENERGY, 0);
        entityData.define(CAPACITY, 1);
        entityData.define(ENERGY_FLOW, 0);
    }

    public boolean isCloaked() {
        return entityData.get(CLOAKED);
    }

    public void setCloaked(boolean cloaked) {
        entityData.set(CLOAKED, cloaked);
    }

    public boolean isShielded() {
        return entityData.get(SHIELDED);
    }

    public void setShielded(boolean shielded) {
        entityData.set(SHIELDED, shielded);
    }

    public boolean isLightsOn() {
        return entityData.get(LIGHTS_ON);
    }

    public void setLightsOn(boolean lightsOn) {
        entityData.set(LIGHTS_ON, lightsOn);
    }

    public boolean isPodsOut() {
        return entityData.get(PODS_OUT);
    }

    public int hurtTime() {
        return entityData.get(HURT_TIME);
    }

    public int hurtDirection() {
        return entityData.get(HURT_DIRECTION);
    }

    public float hurtWobble() {
        return entityData.get(HURT_WOBBLE);
    }

    public JumperEnergy energy() {
        return energy;
    }

    public JumperHull hull() {
        return hull;
    }

    public JumperSystems systems() {
        return systems;
    }

    public JumperWeapons weapons() {
        return weapons;
    }

    public JumperLights lights() {
        return lights;
    }

    public JumperCargo cargo() {
        return cargo;
    }

    public JumperAnimation animation() {
        return animation;
    }

    JumperPods pods() {
        return pods;
    }

    Vec3 serverVelocity() {
        return serverVelocity;
    }

    public RandomSource getRandom() {
        return random;
    }

    public boolean enginesRunning() {
        return energy.hasEnergy() && !onGround();
    }

    public boolean isSubmerged() {
        return getFluidHeight(FluidTags.WATER) > JumperGeometry.CENTER_HEIGHT;
    }

    /** True if any lava (source or flowing, e.g. poured from a bucket) reaches into the hull. */
    public boolean touchesLava() {
        if (lavaCheckTick != tickCount) {
            lavaCheckTick = tickCount;
            touchesLava = findLava();
        }
        return touchesLava;
    }

    public JumperMedium medium() {
        if (touchesLava()) return JumperMedium.LAVA;
        return isSubmerged() ? JumperMedium.WATER : JumperMedium.AIR;
    }

    private boolean findLava() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double offset : segmentOffsets()) {
            AABB box = segmentBox(offset);
            for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
                for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
                    for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
                        FluidState fluid = level().getFluidState(pos.set(x, y, z));
                        if (fluid.is(FluidTags.LAVA) && y + fluid.getHeight(level(), pos) > box.minY) return true;
                    }
                }
            }
        }
        return false;
    }

    public Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    public Vec3 left() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
    }

    public Vec3 modelToWorld(double modelX, double modelY, double modelZ) {
        double scale = JumperGeometry.MODEL_SCALE;
        return position().add(left().scale(modelX * scale)).add(forward().scale(modelZ * scale)).add(0, modelY * scale, 0);
    }

    AABB segmentBox(double offset) {
        Vec3 center = position().add(forward().scale(offset));
        double halfWidth = JumperGeometry.WIDTH / 2.0;
        return new AABB(center.x - halfWidth, center.y, center.z - halfWidth,
                center.x + halfWidth, center.y + JumperGeometry.HEIGHT, center.z + halfWidth);
    }

    static double[] segmentOffsets() {
        return new double[]{JumperGeometry.SEGMENT_OFFSET, 0, -JumperGeometry.SEGMENT_OFFSET};
    }

    public AABB fullBoundingBox() {
        return getBoundingBox()
                .minmax(segmentBox(JumperGeometry.SEGMENT_OFFSET))
                .minmax(segmentBox(-JumperGeometry.SEGMENT_OFFSET));
    }

    public boolean isValidTarget(@Nullable Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isSpectator() || entity == this || hasPassenger(entity)) return false;
        if (entity instanceof PuddleJumperEntity other) return !other.isCloaked();
        return entity instanceof LivingEntity;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(CULLING_MARGIN_HORIZONTAL, CULLING_MARGIN_VERTICAL, CULLING_MARGIN_HORIZONTAL);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts == null) return;
        for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) energy.initialize();
    }

    @Override
    public void tick() {
        if (!level().isClientSide) energy.initialize();
        hull.tickHurtAnimation();
        super.tick();
        interpolation.tick();
        flight.tick();
        updateParts();
        setOnGround(flight.probeGround());
        if (level().isClientSide) {
            animation.tick();
            clientTicker.accept(this);
        } else {
            serverTick();
        }
    }

    private void serverTick() {
        trackServerVelocity();
        pods.tick();
        // passengers are protected by the hull (see PassengerEvents); lava would keep setting them on fire
        getPassengers().forEach(passenger -> {
            passenger.resetFallDistance();
            passenger.clearFire();
        });
        hull.tick();
        energy.tick();
        lights.tick();
        cargo.tick();
    }

    private void trackServerVelocity() {
        Vec3 position = position();
        serverVelocity = position.subtract(lastServerPosition);
        if (serverVelocity.lengthSqr() > TELEPORT_DISTANCE_SQR) serverVelocity = Vec3.ZERO;
        lastServerPosition = position;
    }

    private void updateParts() {
        Vec3 offset = forward().scale(JumperGeometry.SEGMENT_OFFSET);
        movePart(bow, position().add(offset));
        movePart(stern, position().subtract(offset));
    }

    private static void movePart(PuddleJumperPart part, Vec3 target) {
        part.xo = part.xOld = part.getX();
        part.yo = part.yOld = part.getY();
        part.zo = part.zOld = part.getZ();
        part.setPos(target);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        interpolation.lerpTo(x, y, z, yRot);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player pilot ? pilot : null;
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
        Vec3 seat = position().add(forward().scale(JumperGeometry.PILOT_SEAT_Z * JumperGeometry.MODEL_SCALE));
        move.accept(passenger, seat.x, seat.y + getPassengersRidingOffset() + passenger.getMyRidingOffset(), seat.z);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 location = JumperDismount.find(this, passenger);
        return location != null ? location : super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) openCustomInventoryScreen(player);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isVehicle()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!level().isClientSide) cargo.open(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return hull.hurt(source, amount);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == SHIELD_HIT_EVENT) animation.flashShield();
        else super.handleEntityEvent(id);
    }

    @Override
    public void animateHurt(float yaw) {
        hull.animateHurt();
    }

    boolean isFriendlyDrone(DamageSource source) {
        return source.getDirectEntity() instanceof DroneEntity drone && drone.isFriendly(this);
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
    public void push(double x, double y, double z) {}

    @Override
    public void push(Entity other) {}

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

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
        return new ItemStack(ModItems.PUDDLE_JUMPER.get());
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * EYE_HEIGHT_FACTOR;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) lights.clear();
        super.remove(reason);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!isSilent()) level().playSound(null, getX(), getY(), getZ(), sound, getSoundSource(), volume, pitch);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        systems.load(tag);
        hull.load(tag);
        energy.load(tag);
        lights.load(tag);
        cargo.load(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        systems.save(tag);
        hull.save(tag);
        energy.save(tag);
        lights.save(tag);
        cargo.save(tag);
    }
}
