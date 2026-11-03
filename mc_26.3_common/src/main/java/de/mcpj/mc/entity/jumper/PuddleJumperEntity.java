package de.mcpj.mc.entity.jumper;

import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.util.Convert;
import de.mcpj.mc.util.NbtSaveData;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.jumper.Jumper;
import de.mcpj.core.jumper.JumperAction;
import de.mcpj.core.jumper.JumperAnimation;
import de.mcpj.core.jumper.JumperEnergy;
import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.core.jumper.JumperHost;
import de.mcpj.core.jumper.JumperHull;
import de.mcpj.core.jumper.JumperLights;
import de.mcpj.core.jumper.JumperMedium;
import de.mcpj.core.jumper.JumperParticle;
import de.mcpj.core.jumper.JumperSound;
import de.mcpj.core.jumper.JumperSync;
import de.mcpj.core.jumper.JumperWeapons;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Box;
import de.mcpj.core.math.Vec3d;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.network.JumperActionPacket;
import de.mcpj.mc.network.JumperImpactPacket;
import de.mcpj.mc.network.PilotSoundPacket;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * The Puddle Jumper in Minecraft 26.3: the game logic lives in the core ({@link Jumper}); this entity is
 * its host (body, world, sync, effects) and handles what is Minecraft-specific: hull segments as part entities,
 * passengers, interaction, network interpolation, dimension travel and saving.
 */
public abstract class PuddleJumperEntity extends Entity implements HasCustomInventoryScreen, JumperHost, JumperSync {
    private static final EntityDataAccessor<Boolean> CLOAKED = define(EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHIELDED = define(EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LIGHTS_ON = define(EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PODS_OUT = define(EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> HURT_TIME = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HURT_DIRECTION = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HURT_WOBBLE = define(EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HULL_INTEGRITY = define(EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ENERGY = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAPACITY = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENERGY_FLOW = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3fc> ARRIVAL_VELOCITY = define(EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> ARRIVALS = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CONSTRUCTION = define(EntityDataSerializers.FLOAT);

    private static final byte SHIELD_HIT_EVENT = 60;
    /** Eye height relative to the ship's height (the loaders pass it to the entity type builder). */
    public static final float EYE_HEIGHT_FACTOR = 0.6f;
    /** How far the seated pilot sits below the seat position (as in Minecraft 1.20.1). */
    private static final double PILOT_SEAT_DROP = 0.35;
    private static final int INTERPOLATION_STEPS = 3;
    private static final double CULLING_MARGIN_HORIZONTAL = 4.5;
    private static final double CULLING_MARGIN_VERTICAL = 1.0;
    private static final float MAX_STEP_HEIGHT = 1.0f;
    private static final double AUTO_TARGET_MIN_ALIGNMENT = 0.3;
    private static final double NO_LAVA = -1;

    public static Consumer<PuddleJumperEntity> clientTicker = jumper -> {};


    private final Jumper core = new Jumper(this);
    private final JumperCargo cargo = new JumperCargo(this);
    private final JumperTravel travel = new JumperTravel(this);
    private final JumperMeleeAttackers meleeAttackers = new JumperMeleeAttackers(this);
    private final JumperLightning lightning = new JumperLightning(this);
    private final JumperConstruction construction = new JumperConstruction(this);
    private final RandomGenerator randomGenerator = () -> random.nextLong();

    public PuddleJumperEntity(EntityType<? extends PuddleJumperEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        blocksBuilding = true;
    }

    private static <T> EntityDataAccessor<T> define(EntityDataSerializer<T> serializer) {
        return SynchedEntityData.defineId(PuddleJumperEntity.class, serializer);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
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
        entityData.define(ARRIVAL_VELOCITY, new Vector3f());
        entityData.define(ARRIVALS, 0);
        entityData.define(CONSTRUCTION, JumperConstruction.DONE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(ARRIVALS) && level().isClientSide() && entityData.get(ARRIVALS) > 0) core.onArrivalSynced();
    }

    // ------------------------------------------------------------------ core access (for the rest of the mod)

    public Jumper core() {
        return core;
    }

    /** Being built by a projector (see JumperConstruction). */
    public JumperConstruction construction() {
        return construction;
    }

    /** Synced build progress 0..1, or JumperConstruction.DONE for a finished ship. */
    public float constructionProgress() {
        return entityData.get(CONSTRUCTION);
    }

    void setConstructionProgress(float progress) {
        entityData.set(CONSTRUCTION, progress);
    }

    public JumperLightning lightning() {
        return lightning;
    }

    /** Lightning charges the ship instead of hurting or burning it (see JumperLightning). */
    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        lightning.struck(bolt);
    }

    public JumperEnergy energy() {
        return core.energy();
    }

    public JumperHull hull() {
        return core.hull();
    }

    public JumperWeapons weapons() {
        return core.weapons();
    }

    public JumperLights lights() {
        return core.lights();
    }

    public JumperAnimation animation() {
        return core.animation();
    }

    public JumperCargo cargo() {
        return cargo;
    }

    public boolean isCloaked() {
        return cloaked();
    }

    public boolean isShielded() {
        return shielded();
    }

    public boolean isLightsOn() {
        return lightsOn();
    }

    public boolean isPodsOut() {
        return podsOut();
    }

    public boolean enginesRunning() {
        return core.enginesRunning();
    }

    public boolean isSubmerged() {
        return core.isSubmerged();
    }

    public boolean touchesLava() {
        return core.touchesLava();
    }

    public JumperMedium medium() {
        return core.medium();
    }

    public Vec3 forward() {
        return Convert.toMc(core.forward());
    }

    public Vec3 left() {
        return Convert.toMc(core.left());
    }

    public AABB fullBoundingBox() {
        return Convert.toMc(core.fullBox());
    }

    public RandomSource getRandom() {
        return random;
    }

    /** A pilot action from the network; only the pilot may act. */
    public void performPilotAction(ServerPlayer pilot, JumperAction action, int targetId) {
        if (getControllingPassenger() != pilot) return;
        core.systems().perform(action, Convert.toCore(pilot.getLookAngle()), targetId);
    }

    public boolean isValidTarget(@Nullable Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isSpectator() || entity == this || hasPassenger(entity)) return false;
        if (entity instanceof PuddleJumperEntity other) return !other.isCloaked();
        return entity instanceof LivingEntity;
    }

    // ------------------------------------------------------------------ JumperSync (entity data)

    @Override
    public boolean cloaked() {
        return entityData.get(CLOAKED);
    }

    @Override
    public void setCloaked(boolean cloaked) {
        entityData.set(CLOAKED, cloaked);
    }

    @Override
    public boolean shielded() {
        return entityData.get(SHIELDED);
    }

    @Override
    public void setShielded(boolean shielded) {
        entityData.set(SHIELDED, shielded);
    }

    @Override
    public boolean lightsOn() {
        return entityData.get(LIGHTS_ON);
    }

    @Override
    public void setLightsOn(boolean lightsOn) {
        entityData.set(LIGHTS_ON, lightsOn);
    }

    @Override
    public boolean podsOut() {
        return entityData.get(PODS_OUT);
    }

    @Override
    public void setPodsOut(boolean podsOut) {
        entityData.set(PODS_OUT, podsOut);
    }

    @Override
    public int hurtTime() {
        return entityData.get(HURT_TIME);
    }

    @Override
    public void setHurtTime(int ticks) {
        entityData.set(HURT_TIME, ticks);
    }

    @Override
    public int hurtDirection() {
        return entityData.get(HURT_DIRECTION);
    }

    @Override
    public void setHurtDirection(int direction) {
        entityData.set(HURT_DIRECTION, direction);
    }

    @Override
    public float hurtWobble() {
        return entityData.get(HURT_WOBBLE);
    }

    @Override
    public void setHurtWobble(float wobble) {
        entityData.set(HURT_WOBBLE, wobble);
    }

    @Override
    public float hullIntegrity() {
        return entityData.get(HULL_INTEGRITY);
    }

    @Override
    public void setHullIntegrity(float integrity) {
        entityData.set(HULL_INTEGRITY, integrity);
    }

    @Override
    public int storedEnergy() {
        return entityData.get(ENERGY);
    }

    @Override
    public void setStoredEnergy(int energy) {
        entityData.set(ENERGY, energy);
    }

    @Override
    public int capacity() {
        return entityData.get(CAPACITY);
    }

    @Override
    public void setCapacity(int capacity) {
        entityData.set(CAPACITY, capacity);
    }

    @Override
    public int energyFlow() {
        return entityData.get(ENERGY_FLOW);
    }

    @Override
    public void setEnergyFlow(int flow) {
        entityData.set(ENERGY_FLOW, flow);
    }

    @Override
    public Vec3d arrivalVelocity() {
        Vector3fc velocity = entityData.get(ARRIVAL_VELOCITY);
        return new Vec3d(velocity.x(), velocity.y(), velocity.z());
    }

    @Override
    public int arrivals() {
        return entityData.get(ARRIVALS);
    }

    @Override
    public void announceArrival(Vec3d velocity) {
        entityData.set(ARRIVAL_VELOCITY, new Vector3f((float) velocity.x(), (float) velocity.y(), (float) velocity.z()));
        entityData.set(ARRIVALS, arrivals() + 1);
    }

    // ------------------------------------------------------------------ JumperHost: body

    @Override
    public Vec3d location() {
        return new Vec3d(getX(), getY(), getZ());
    }

    @Override
    public Vec3d previousLocation() {
        return new Vec3d(xo, yo, zo);
    }

    @Override
    public float yaw() {
        return getYRot();
    }

    @Override
    public float previousYaw() {
        return yRotO;
    }

    @Override
    public void setYaw(float yaw) {
        setYRot(yaw);
    }

    @Override
    public Vec3d velocity() {
        return Convert.toCore(getDeltaMovement());
    }

    @Override
    public void setVelocity(Vec3d velocity) {
        setDeltaMovement(Convert.toMc(velocity));
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide();
    }

    @Override
    public boolean isControlledLocally() {
        return isLocalInstanceAuthoritative();
    }

    @Override
    public boolean hasPilot() {
        return getControllingPassenger() != null;
    }

    @Override
    public boolean hasPassengers() {
        return isVehicle();
    }

    @Override
    public int tickCount() {
        return tickCount;
    }

    @Override
    public long gameTime() {
        return level().getGameTime();
    }

    @Override
    public RandomGenerator random() {
        return randomGenerator;
    }

    @Override
    public JumperSync sync() {
        return this;
    }

    // ------------------------------------------------------------------ JumperHost: world

    @Override
    public Vec3d collide(Vec3d wanted, Box segment) {
        return Convert.toCore(Entity.collideBoundingBox(this, Convert.toMc(wanted), Convert.toMc(segment), level(), List.of()));
    }

    @Override
    public Vec3d moveBy(Vec3d movement) {
        Vec3 before = position();
        move(MoverType.SELF, Convert.toMc(movement));
        return Convert.toCore(position().subtract(before));
    }

    @Override
    public boolean isFree(Box box) {
        return level().noCollision(this, Convert.toMc(box));
    }

    @Override
    public double waterHeight() {
        return getFluidHeight(FluidTags.WATER);
    }

    @Override
    public double lavaHeight(BlockPosition pos) {
        BlockPos blockPos = Convert.toMc(pos);
        FluidState fluid = level().getFluidState(blockPos);
        return fluid.is(FluidTags.LAVA) ? fluid.getHeight(level(), blockPos) : NO_LAVA;
    }

    @Override
    public int chargeFromPad(BlockPosition pos, int room, int intervalTicks, float chargeFraction) {
        return level().getBlockEntity(Convert.toMc(pos)) instanceof ChargingPadBlockEntity pad
                ? pad.supplyJumper(room, intervalTicks, chargeFraction) : 0;
    }

    @Override
    public boolean placeLight(BlockPosition position, int lightLevel) {
        BlockPos pos = Convert.toMc(position);
        Level level = level();
        if (!level.isLoaded(pos)) return false;
        BlockState existing = level.getBlockState(pos);
        boolean water = existing.is(Blocks.WATER) && existing.getFluidState().isSource();
        if (!existing.isAir() && !water) return false;
        BlockState light = Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, lightLevel)
                .setValue(BlockStateProperties.WATERLOGGED, water);
        return level.setBlock(pos, light, Block.UPDATE_CLIENTS);
    }

    @Override
    public void removeLight(BlockPosition position) {
        BlockPos pos = Convert.toMc(position);
        Level level = level();
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.LIGHT)) return;
        BlockState restored = state.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, restored, Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ JumperHost: effects and actions

    @Override
    public void playSound(JumperSound sound, float volume, float pitch) {
        playSound(JumperEffects.sound(sound), volume, pitch);
    }

    /** Everyone else hears ship sounds at the ship; the pilot hears them in the cockpit (see PilotSoundPacket). */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (isSilent()) return;
        ServerPlayer pilot = getControllingPassenger() instanceof ServerPlayer player ? player : null;
        level().playSound(pilot, getX(), getY(), getZ(), sound, getSoundSource(), volume, pitch);
        if (pilot != null) McpjPlatform.get().sendToPlayer(pilot, new PilotSoundPacket(sound, volume, pitch));
    }

    @Override
    public void serverParticles(JumperParticle particle, Vec3d at, int count, Vec3d spread, double speed) {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(JumperEffects.particle(particle), at.x(), at.y(), at.z(), count, spread.x(), spread.y(), spread.z(), speed);
        }
    }

    @Override
    public void clientParticle(JumperParticle particle, Vec3d at, Vec3d velocity) {
        level().addParticle(JumperEffects.particle(particle), at.x(), at.y(), at.z(), velocity.x(), velocity.y(), velocity.z());
    }

    @Override
    public void showShieldHit() {
        level().broadcastEntityEvent(this, SHIELD_HIT_EVENT);
    }

    @Override
    public void hullDamaged(@Nullable Object attacker) {
        gameEvent(GameEvent.ENTITY_DAMAGE, attacker instanceof Entity entity ? entity : null);
    }

    @Override
    public void loseMobPursuers(double range) {
        for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(range))) {
            if (mob.getTarget() != null && hasPassenger(mob.getTarget())) mob.setTarget(null);
        }
    }

    @Override
    public void launchDrone(Vec3d spawn, Vec3d velocity, int targetId) {
        if (!(getControllingPassenger() instanceof ServerPlayer pilot)) return;
        DroneEntity drone = new DroneEntity(level(), pilot, this, resolveTarget(targetId));
        drone.setPos(Convert.toMc(spawn));
        drone.setDeltaMovement(Convert.toMc(velocity));
        level().addFreshEntity(drone);
    }

    @Nullable
    private Entity resolveTarget(int targetId) {
        Entity selected = targetId == JumperActionPacket.NO_TARGET ? null : level().getEntity(targetId);
        double lockRange = ServerConfig.TARGETING.lockRange();
        if (isValidTarget(selected) && selected.distanceToSqr(this) <= lockRange * lockRange) return selected;
        return findAutoTarget();
    }

    @Nullable
    private LivingEntity findAutoTarget() {
        Vec3 forward = forward();
        Vec3 center = position();
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(ServerConfig.TARGETING.autoTargetRange()),
                        entity -> entity instanceof Enemy && entity.isAlive() && !hasPassenger(entity)
                                && entity.position().subtract(center).normalize().dot(forward) > AUTO_TARGET_MIN_ALIGNMENT)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this)))
                .orElse(null);
    }

    @Override
    public void reportImpact(float horizontalSpeed, float fallSpeed) {
        McpjPlatform.get().sendToServer(new JumperImpactPacket(horizontalSpeed, fallSpeed));
    }

    // ------------------------------------------------------------------ JumperHost: destruction

    @Override
    public void explode(Vec3d at, float power) {
        level().explode(this, at.x(), at.y(), at.z(), power, Level.ExplosionInteraction.MOB);
    }

    @Override
    public boolean entityDropsEnabled() {
        return level() instanceof ServerLevel level && level.getGameRules().get(GameRules.ENTITY_DROPS);
    }

    @Override
    public void dropShipItem() {
        if (level() instanceof ServerLevel level) spawnAtLocation(level, McpjPlatform.get().jumperItem());
    }

    @Override
    public void dropCargo() {
        cargo.dropAll();
    }

    // ------------------------------------------------------------------ Minecraft entity

    /** The box the renderer culls the ship with (the model reaches beyond the hull). */
    public AABB cullingBox() {
        return getBoundingBox().inflate(CULLING_MARGIN_HORIZONTAL, CULLING_MARGIN_VERTICAL, CULLING_MARGIN_HORIZONTAL);
    }

    /** The loader calls this when the ship was added to a level. */
    protected void onJoinedLevel() {
        core.onAdded();
    }

    /** The bow and stern hull segments (each loader has its own segment entities). */
    public abstract List<? extends Entity> segments();

    /** Moves the bow and stern hull segments (each loader has its own segment entities). */
    protected abstract void moveSegments(Vec3 bowCenter, Vec3 sternCenter);

    @Override
    public void tick() {
        if (construction.isActive()) {
            // under construction: frozen, no ship systems (see JumperConstruction)
            super.tick();
            updateParts();
            construction.tick();
            return;
        }
        core.beforeTick();
        super.tick();
        core.flightTick();
        updateParts();
        setOnGround(core.probeGround());
        if (level().isClientSide()) {
            core.clientTick();
            clientTicker.accept(this);
        } else {
            serverTick();
        }
    }

    private void serverTick() {
        core.trackServerVelocity();
        travel.tick();
        meleeAttackers.tick();
        // passengers are protected by the hull (see PassengerEvents); lava would keep setting them on fire
        getPassengers().forEach(passenger -> {
            passenger.resetFallDistance();
            passenger.clearFire();
        });
        core.serverTick();
        cargo.tick();
    }

    private void updateParts() {
        Vec3 offset = forward().scale(JumperGeometry.SEGMENT_OFFSET);
        moveSegments(position().add(offset), position().subtract(offset));
    }

    /** Moves a segment entity, keeping its previous position for interpolation. */
    protected static void moveSegment(Entity segment, Vec3 target) {
        segment.xo = segment.xOld = segment.getX();
        segment.yo = segment.yOld = segment.getY();
        segment.zo = segment.zOld = segment.getZ();
        segment.setPos(target);
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, INTERPOLATION_STEPS);
    }

    @Override
    public float maxUpStep() {
        return MAX_STEP_HEIGHT;
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
    protected void positionRider(Entity passenger, MoveFunction move) {
        if (!hasPassenger(passenger)) return;
        Vec3d seat = core.seatPosition();
        move.accept(passenger, seat.x(), seat.y() - PILOT_SEAT_DROP, seat.z());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 location = JumperDismount.find(this, passenger);
        return location != null ? location : super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (construction.isActive()) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide()) openCustomInventoryScreen(player);
            return InteractionResult.SUCCESS;
        }
        if (isVehicle()) return InteractionResult.PASS;
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!level().isClientSide()) cargo.open(player);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableToBase(source) || construction.isActive()) return false;
        Entity attacker = source.getEntity();
        return core.hull().hurt(new JumperHull.Damage(amount, attacker, attacker != null && hasPassenger(attacker),
                isFriendlyDrone(source), source.getDirectEntity() instanceof DroneEntity drone ? drone.getUUID() : null,
                source.is(DamageTypeTags.IS_EXPLOSION), attacker instanceof Player player && player.getAbilities().instabuild));
    }

    private boolean isFriendlyDrone(DamageSource source) {
        return source.getDirectEntity() instanceof DroneEntity drone && drone.isFriendly(this);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == SHIELD_HIT_EVENT) core.animation().flashShield();
        else super.handleEntityEvent(id);
    }

    @Override
    public void animateHurt(float yaw) {
        core.hull().animateHurt();
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
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        if (other instanceof JumperSegment segment && segment.jumper() == this) return false;
        return super.canCollideWith(other);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(McpjPlatform.get().jumperItem());
    }

    /** Portals, stargates and commands: the passengers stay on board and the ship keeps flying (see JumperTravel). */
    @Nullable
    @Override
    public Entity teleport(TeleportTransition transition) {
        JumperTravel.Departure departure = travel.depart();
        Entity arrived = super.teleport(transition);
        if (arrived == this) travel.teleportedWithinDimension(departure);
        else if (arrived instanceof PuddleJumperEntity jumper) jumper.travel.arriveFromOtherDimension(departure);
        else travel.travelFailed(departure);
        return arrived;
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        JumperTravel.Departure departure = travel.depart();
        super.teleportTo(x, y, z);
        if (!level().isClientSide()) travel.teleportedWithinDimension(departure);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) core.onRemoved();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        NbtSaveData data = NbtSaveData.reading(input);
        core.systems().load(data);
        construction.load(data);
        core.hull().load(data);
        core.energy().load(data);
        core.lights().load(data);
        cargo.load(input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        NbtSaveData data = NbtSaveData.writing(output);
        core.systems().save(data);
        construction.save(data);
        core.hull().save(data);
        core.energy().save(data);
        core.lights().save(data);
        cargo.save(output);
    }
}
