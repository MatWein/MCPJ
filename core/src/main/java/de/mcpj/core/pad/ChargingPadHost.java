package de.mcpj.core.pad;

/** The block in the world around a {@link ChargingPad}. */
public interface ChargingPadHost {
    /** The stored energy changed (save the block). */
    void markChanged();

    /** The comparator output changed (update neighbours). */
    void comparatorChanged();

    PadMode currentMode();

    void setMode(PadMode mode);
}
