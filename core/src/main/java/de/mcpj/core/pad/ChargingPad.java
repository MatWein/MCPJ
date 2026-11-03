package de.mcpj.core.pad;

import de.mcpj.core.config.ChargingPadConfig;
import de.mcpj.core.energy.EnergyInput;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.util.SaveData;

/**
 * Jumper charging pad: buffers energy from any energy mod (receive only) and hands it to a landed jumper.
 * Unless the config requires power, it also charges on its own (slower) when its buffer is empty.
 * A comparator reads the charge of the jumper currently charging.
 */
public final class ChargingPad implements EnergyInput {
    private static final int CHARGING_HOLD_TICKS = 10;
    private static final int STATE_UPDATE_INTERVAL = 5;
    private static final int MAX_COMPARATOR_LEVEL = 15;
    private static final String ENERGY_TAG = "Energy";

    private final ChargingPadHost host;
    private int stored;
    private int chargingTimer;
    private int comparatorLevel;

    public ChargingPad(ChargingPadHost host) {
        this.host = host;
    }

    public void serverTick(long gameTime) {
        if (chargingTimer > 0 && --chargingTimer == 0) setComparatorLevel(0);
        if (gameTime % STATE_UPDATE_INTERVAL != 0) return;
        PadMode mode = currentMode();
        if (host.currentMode() != mode) host.setMode(mode);
    }

    private PadMode currentMode() {
        if (chargingTimer > 0) return PadMode.CHARGING;
        return stored > 0 || !ServerConfig.CHARGING_PAD.requirePower() ? PadMode.POWERED : PadMode.OFF;
    }

    /** A landed jumper asks for energy; returns what it gets (limited by the pad's charge rate). */
    public int supplyJumper(int requested, int ticks, float jumperChargeFraction) {
        chargingTimer = CHARGING_HOLD_TICKS;
        setComparatorLevel(Math.round(jumperChargeFraction * MAX_COMPARATOR_LEVEL));
        ChargingPadConfig config = ServerConfig.CHARGING_PAD;
        if (!config.requirePower()) {
            // without (enough) energy from cables the pad charges on its own; buffered energy goes first, at full rate
            int free = Math.min(requested, config.freeChargeRate() * ticks);
            if (stored < free) return free;
        }
        return drain(Math.min(requested, config.chargeRate() * ticks));
    }

    @Override
    public int receive(int amount, boolean simulate) {
        int accepted = Math.min(Math.min(amount, ServerConfig.CHARGING_PAD.inputRate()), capacity() - stored);
        if (accepted <= 0) return 0;
        if (!simulate) {
            stored += accepted;
            host.markChanged();
        }
        return accepted;
    }

    private int drain(int amount) {
        int drained = Math.min(amount, stored);
        if (drained > 0) {
            stored -= drained;
            host.markChanged();
        }
        return drained;
    }

    @Override
    public int stored() {
        return stored;
    }

    @Override
    public void restoreStored(int energy) {
        stored = Math.max(0, energy);
        host.markChanged();
    }

    @Override
    public int capacity() {
        return ServerConfig.CHARGING_PAD.buffer();
    }

    public int comparatorLevel() {
        return comparatorLevel;
    }

    private void setComparatorLevel(int level) {
        if (level == comparatorLevel) return;
        comparatorLevel = level;
        host.comparatorChanged();
    }

    public void load(SaveData data) {
        stored = Math.max(0, data.getInt(ENERGY_TAG));
    }

    public void save(SaveData data) {
        data.putInt(ENERGY_TAG, stored);
    }
}
