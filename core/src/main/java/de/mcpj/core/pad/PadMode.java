package de.mcpj.core.pad;

/** What the charging pad shows: dark, glowing runway lights, or bright while a jumper charges. */
public enum PadMode {
    OFF(0),
    POWERED(10),
    CHARGING(15);

    private final int lightLevel;

    PadMode(int lightLevel) {
        this.lightLevel = lightLevel;
    }

    public int lightLevel() {
        return lightLevel;
    }
}
