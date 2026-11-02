package de.mcpj.entity.jumper;

import de.mcpj.block.ChargingPadBlockEntity;
import de.mcpj.config.EnergyConfig;
import de.mcpj.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

import static de.mcpj.entity.jumper.PuddleJumperEntity.CAPACITY;
import static de.mcpj.entity.jumper.PuddleJumperEntity.ENERGY;
import static de.mcpj.entity.jumper.PuddleJumperEntity.ENERGY_FLOW;

public final class JumperEnergy {
    private static final int CHARGE_INTERVAL = 5;
    private static final float FLOW_SMOOTHING = 0.1f;
    private static final double PAD_PROBE_DEPTH = 0.05;
    private static final double PAD_PROBE_INSET = 0.05;
    private static final String ENERGY_TAG = "Energy";
    private static final String CAPACITY_TAG = "Capacity";

    private final PuddleJumperEntity jumper;
    private boolean initialized;
    private int chargePerTick;
    private float smoothedFlow;

    JumperEnergy(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    public int stored() {
        return jumper.getEntityData().get(ENERGY);
    }

    public int capacity() {
        return Math.max(1, jumper.getEntityData().get(CAPACITY));
    }

    public float fraction() {
        return stored() / (float) capacity();
    }

    public boolean hasEnergy() {
        return stored() > 0;
    }

    /** Smoothed net flow in FE/tick: positive = consumption, negative = charging. */
    public int flow() {
        return jumper.getEntityData().get(ENERGY_FLOW);
    }

    public void set(int energy) {
        initialized = true;
        jumper.getEntityData().set(ENERGY, Mth.clamp(energy, 0, capacity()));
    }

    public boolean tryConsume(int amount) {
        if (stored() < amount) return false;
        set(stored() - amount);
        return true;
    }

    /** Consumes the amount; if it is not available the storage is emptied and false is returned. */
    boolean drain(int amount) {
        if (tryConsume(amount)) return true;
        set(0);
        return false;
    }

    void initialize() {
        int capacity = ServerConfig.ENERGY.capacity();
        if (jumper.getEntityData().get(CAPACITY) != capacity) {
            jumper.getEntityData().set(CAPACITY, capacity);
            if (stored() > capacity) set(capacity);
        }
        if (!initialized) set((int) Math.round(capacity * ServerConfig.ENERGY.startCharge()));
    }

    void tick() {
        int consumption = (int) Math.round(drivePower() + systemsPower());
        if (consumption > 0 && !drain(consumption)) jumper.systems().powerFailure();

        if (!jumper.onGround()) chargePerTick = 0;
        else if (jumper.tickCount % CHARGE_INTERVAL == 0) chargePerTick = chargeFromPads() / CHARGE_INTERVAL;

        smoothedFlow += ((consumption - chargePerTick) - smoothedFlow) * FLOW_SMOOTHING;
        jumper.getEntityData().set(ENERGY_FLOW, Math.round(smoothedFlow));
    }

    private double drivePower() {
        if (jumper.onGround() || !hasEnergy()) return 0;
        EnergyConfig config = ServerConfig.ENERGY;
        double speedShare = Mth.clamp(jumper.serverVelocity().length() / ServerConfig.FLIGHT.maxSpeedPerTick(), 0, 1);
        double power = Mth.lerp(speedShare, config.hoverDrain(), config.thrustDrain());
        return power * config.driveFactor(jumper.medium());
    }

    private double systemsPower() {
        EnergyConfig config = ServerConfig.ENERGY;
        JumperMedium medium = jumper.medium();
        double underFire = jumper.hull().isUnderFire() ? config.underFireFactor() : 1.0;
        double power = 0;
        if (jumper.isShielded()) power += config.shieldDrain() * config.shieldFactor(medium) * underFire;
        if (jumper.isCloaked()) power += config.cloakDrain() * config.cloakFactor(medium) * underFire;
        if (jumper.isLightsOn() && !jumper.isCloaked()) power += config.lightDrain();
        return power;
    }

    private int chargeFromPads() {
        int room = capacity() - stored();
        float chargeFraction = fraction();
        int gained = 0;
        for (BlockPos pos : blocksBelowHull()) {
            if (jumper.level().getBlockEntity(pos) instanceof ChargingPadBlockEntity pad) {
                gained += pad.supplyJumper(room - gained, CHARGE_INTERVAL, chargeFraction);
            }
        }
        if (gained > 0) set(stored() + gained);
        return gained;
    }

    private Set<BlockPos> blocksBelowHull() {
        Set<BlockPos> below = new HashSet<>();
        int y = Mth.floor(jumper.getY() - PAD_PROBE_DEPTH);
        for (double offset : PuddleJumperEntity.segmentOffsets()) {
            AABB segment = jumper.segmentBox(offset);
            for (int x = Mth.floor(segment.minX + PAD_PROBE_INSET); x <= Mth.floor(segment.maxX - PAD_PROBE_INSET); x++) {
                for (int z = Mth.floor(segment.minZ + PAD_PROBE_INSET); z <= Mth.floor(segment.maxZ - PAD_PROBE_INSET); z++) {
                    below.add(new BlockPos(x, y, z));
                }
            }
        }
        return below;
    }

    void load(CompoundTag tag) {
        if (!tag.contains(ENERGY_TAG)) return;
        jumper.getEntityData().set(CAPACITY, Math.max(1, tag.getInt(CAPACITY_TAG)));
        set(tag.getInt(ENERGY_TAG));
    }

    void save(CompoundTag tag) {
        if (!initialized) return;
        tag.putInt(ENERGY_TAG, stored());
        tag.putInt(CAPACITY_TAG, capacity());
    }
}
