package de.mcpj.core.hud;

import de.mcpj.core.util.Units;

/** Number and duration formatting for the HUD. */
final class HudFormat {
    private static final int THOUSAND = 1_000;
    private static final int MILLION = 1_000_000;
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_HOUR = 3_600;

    private HudFormat() {}

    static String compact(int value) {
        if (value >= MILLION) return String.format("%.1fM", value / (float) MILLION);
        if (value >= THOUSAND) return String.format("%.1fk", value / (float) THOUSAND);
        return Integer.toString(value);
    }

    static String duration(long ticks) {
        long seconds = ticks / Units.TICKS_PER_SECOND;
        if (seconds >= SECONDS_PER_HOUR) {
            return seconds / SECONDS_PER_HOUR + " h " + (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE + " min";
        }
        if (seconds >= SECONDS_PER_MINUTE) return seconds / SECONDS_PER_MINUTE + " min";
        return seconds + " s";
    }

    /** Meters below 1 km, else kilometers with one decimal. */
    static String distance(double blocks) {
        if (blocks >= THOUSAND) return String.format("%.1f km", blocks / THOUSAND);
        return Math.round(blocks) + " m";
    }

    static String oneDecimal(double value) {
        return String.format("%.1f", value);
    }
}
