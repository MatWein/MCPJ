package de.mcpj.core.projector;

import de.mcpj.core.config.ProjectorConfig;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.energy.EnergyInput;
import de.mcpj.core.util.SaveData;

/**
 * Jumper projector: a redstone pulse starts building a jumper above it (one pulse, one jumper; pulses while building
 * do nothing). The build pauses while the space is blocked or, if the config requires power, energy is missing.
 */
public final class JumperProjector implements EnergyInput {
    private static final int IDLE = -1;
    /** After loading, the jumper may appear a little later than the block; wait before giving up on it. */
    private static final int MISSING_JUMPER_GRACE_TICKS = 100;
    private static final String ENERGY_TAG = "Energy";
    private static final String PROGRESS_TAG = "Progress";
    private static final String SIGNAL_TAG = "Signal";

    private final ProjectorHost host;
    private int stored;
    private int progress = IDLE;
    private boolean signal;
    private int missingJumperTicks;

    public JumperProjector(ProjectorHost host) {
        this.host = host;
    }

    public void serverTick(boolean redstone) {
        boolean pulse = redstone && !signal;
        if (redstone != signal) {
            signal = redstone;
            host.markChanged();
        }
        if (!isBuilding()) {
            if (pulse && host.isSpaceFree() && host.startJumper()) {
                progress = 0;
                missingJumperTicks = 0;
                host.setBuilding(true);
                host.markChanged();
            }
            return;
        }
        if (!host.hasJumper()) {
            if (++missingJumperTicks > MISSING_JUMPER_GRACE_TICKS) stop();
            return;
        }
        missingJumperTicks = 0;
        if (host.isSpaceFree() && consumeEnergy()) {
            progress++;
            host.markChanged();
        }
        if (progress >= buildTicks()) {
            host.finishJumper();
            stop();
        } else {
            host.updateJumper(progressFraction());
        }
    }

    private boolean consumeEnergy() {
        if (!ServerConfig.PROJECTOR.requirePower()) return true;
        int perTick = energyPerTick();
        if (stored < perTick) return false;
        stored -= perTick;
        return true;
    }

    private void stop() {
        progress = IDLE;
        missingJumperTicks = 0;
        host.setBuilding(false);
        host.markChanged();
    }

    public boolean isBuilding() {
        return progress != IDLE;
    }

    /** Build progress 0..1 (0 while idle). */
    public float progressFraction() {
        return isBuilding() ? Math.min(1f, progress / (float) buildTicks()) : 0f;
    }

    /** Energy per tick while building, if the config requires power. */
    public static int energyPerTick() {
        ProjectorConfig config = ServerConfig.PROJECTOR;
        return (int) Math.ceil(config.energyPerJumper() / (double) config.buildTicks());
    }

    private static int buildTicks() {
        return ServerConfig.PROJECTOR.buildTicks();
    }

    /** For game tests: jumps to the given progress of a running build. */
    public void skipTo(float fraction) {
        if (isBuilding()) progress = Math.round(fraction * buildTicks());
    }

    @Override
    public int receive(int amount, boolean simulate) {
        int accepted = Math.min(Math.min(amount, ServerConfig.PROJECTOR.inputRate()), capacity() - stored);
        if (accepted <= 0) return 0;
        if (!simulate) {
            stored += accepted;
            host.markChanged();
        }
        return accepted;
    }

    @Override
    public int stored() {
        return stored;
    }

    @Override
    public int capacity() {
        return ServerConfig.PROJECTOR.buffer();
    }

    @Override
    public void restoreStored(int energy) {
        stored = Math.max(0, energy);
        host.markChanged();
    }

    public void load(SaveData data) {
        stored = Math.max(0, data.getInt(ENERGY_TAG));
        progress = data.contains(PROGRESS_TAG) ? data.getInt(PROGRESS_TAG) : IDLE;
        signal = data.getBoolean(SIGNAL_TAG);
    }

    public void save(SaveData data) {
        data.putInt(ENERGY_TAG, stored);
        data.putInt(PROGRESS_TAG, progress);
        data.putBoolean(SIGNAL_TAG, signal);
    }
}
