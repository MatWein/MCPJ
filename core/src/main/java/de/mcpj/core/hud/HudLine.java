package de.mcpj.core.hud;

import java.util.List;

/** One HUD line: its parts shown one after another, in one RGB color. */
public record HudLine(List<HudText> parts, int color) {
    public static HudLine of(HudText text, int color) {
        return new HudLine(List.of(text), color);
    }
}
