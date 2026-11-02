package de.mcpj.core.hud;

import de.mcpj.core.targeting.TargetKind;

/** Colors of the cockpit HUD and target markers (RGB, ARGB where noted). */
public final class HudColors {
    public static final int TEAL = 0x7FE6F2;
    public static final int GOLD = 0xFFC94A;
    public static final int GREY = 0x9AA8AD;
    public static final int RED = 0xFF5040;
    public static final int DARK_RED = 0x803020;
    public static final int GREEN = 0x7FF29A;
    /** ARGB */
    public static final int PANEL_BACKGROUND = 0x80102028;
    /** ARGB */
    public static final int PANEL_ACCENT = 0xFF000000 | TEAL;

    private static final int MARKER_NEUTRAL = 0x66FFFFFF;
    private static final int MARKER_NEUTRAL_SELECTED = 0xFF000000 | GOLD;
    private static final int MARKER_ENEMY = 0x99FF3030;
    private static final int MARKER_ENEMY_SELECTED = 0xFFFF2A20;
    private static final int MARKER_JUMPER = 0x9960D8FF;
    private static final int MARKER_JUMPER_SELECTED = 0xFF60D8FF;
    private static final int RGB_MASK = 0x00FFFFFF;
    private static final int HALF_ALPHA = 0x80000000;
    private static final int BLINK_HALF_PERIOD = 10;

    private HudColors() {}

    /** ARGB marker color. */
    public static int marker(TargetKind kind, boolean selected) {
        return switch (kind) {
            case ENEMY -> selected ? MARKER_ENEMY_SELECTED : MARKER_ENEMY;
            case JUMPER -> selected ? MARKER_JUMPER_SELECTED : MARKER_JUMPER;
            case NEUTRAL -> selected ? MARKER_NEUTRAL_SELECTED : MARKER_NEUTRAL;
        };
    }

    public static int rgb(int argb) {
        return argb & RGB_MASK;
    }

    public static int halfTransparent(int argb) {
        return rgb(argb) | HALF_ALPHA;
    }

    public static int blink(int tickCount, int on, int off) {
        return (tickCount / BLINK_HALF_PERIOD) % 2 == 0 ? on : off;
    }
}
