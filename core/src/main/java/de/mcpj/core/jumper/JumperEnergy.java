package de.mcpj.core.jumper;

import de.mcpj.core.config.EnergyConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Box;
import de.mcpj.core.math.MathUtil;

import java.util.HashSet;
import java.util.Set;

/** Energy storage (Forge Energy units): drive, shield, cloak and lights drain it, landed ships charge from pads. */
public final class JumperEnergy {
    private static final int CHARGE_INTERVAL = 5;
    private static final float FLOW_SMOOTHING = 0.1f;
    private static final double PAD_PROBE_DEPTH = 0.05;
    private static final double PAD_PROBE_INSET = 0.05;
    private static final String ENERGY_TAG = "Energy";
    private static final String CAPACITY_TAG = "Capacity";

    private final Jumper jumper;
    private boolean initialized;
    private int chargePerTick;
    private float smoothedFlow;

    JumperEnergy(Jumper jumper) {
        this.jumper = jumper;
    }

    public int stored() {
        return jumper.sync().storedEnergy();
    }

    public int capacity() {
        return Math.max(1, jumper.sync().capacity());
    }

    public float fraction() {
        return stored() / (float) capacity();
    }

    public boolean hasEnergy() {
        return stored() > 0;
    }

    /** Smoothed net flow in FE/tick: positive = consumption, negative = charging. */
    public int flow() {
        return jumper.sync().energyFlow();
    }

    public void set(int energy) {
        initialized = true;
        jumper.sync().setStoredEnergy(MathUtil.clamp(energy, 0, capacity()));
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
        if (jumper.sync().capacity() != capacity) {
            jumper.sync().setCapacity(capacity);
            if (stored() > capacity) set(capacity);
        }
        if (!initialized) set((int) Math.round(capacity * ServerConfig.ENERGY.startCharge()));
    }

    void tick() {
        int consumption = (int) Math.round(drivePower() + systemsPower());
        if (consumption > 0 && !drain(consumption)) jumper.systems().powerFailure();

        if (!jumper.host().onGround()) chargePerTick = 0;
        else if (jumper.host().tickCount() % CHARGE_INTERVAL == 0) chargePerTick = chargeFromPads() / CHARGE_INTERVAL;

        smoothedFlow += ((consumption - chargePerTick) - smoothedFlow) * FLOW_SMOOTHING;
        jumper.sync().setEnergyFlow(Math.round(smoothedFlow));
    }

    private double drivePower() {
        if (jumper.host().onGround() || !hasEnergy()) return 0;
        EnergyConfig config = ServerConfig.ENERGY;
        double speedShare = MathUtil.clamp(jumper.serverVelocity().length() / ServerConfig.FLIGHT.maxSpeedPerTick(), 0, 1);
        double power = MathUtil.lerp(speedShare, config.hoverDrain(), config.thrustDrain());
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
        for (BlockPosition pos : blocksBelowHull()) {
            gained += jumper.host().chargeFromPad(pos, room - gained, CHARGE_INTERVAL, chargeFraction);
        }
        if (gained > 0) set(stored() + gained);
        return gained;
    }

    private Set<BlockPosition> blocksBelowHull() {
        Set<BlockPosition> below = new HashSet<>();
        int y = Jumper.floor(jumper.host().location().y() - PAD_PROBE_DEPTH);
        for (double offset : Jumper.segmentOffsets()) {
            Box segment = jumper.segmentBox(offset);
            for (int x = Jumper.floor(segment.minX() + PAD_PROBE_INSET); x <= Jumper.floor(segment.maxX() - PAD_PROBE_INSET); x++) {
                for (int z = Jumper.floor(segment.minZ() + PAD_PROBE_INSET); z <= Jumper.floor(segment.maxZ() - PAD_PROBE_INSET); z++) {
                    below.add(new BlockPosition(x, y, z));
                }
            }
        }
        return below;
    }

    public void load(SaveData data) {
        if (!data.contains(ENERGY_TAG)) return;
        jumper.sync().setCapacity(Math.max(1, data.getInt(CAPACITY_TAG)));
        set(data.getInt(ENERGY_TAG));
    }

    public void save(SaveData data) {
        if (!initialized) return;
        data.putInt(ENERGY_TAG, stored());
        data.putInt(CAPACITY_TAG, capacity());
    }
}
