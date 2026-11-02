package de.mcpj.core.hud;

/** A piece of HUD text; the platform turns it into its own (translated) text components. */
public sealed interface HudText {
    /** Translated text; arguments are again HUD texts. */
    record Translated(String key, HudText... args) implements HudText {}

    /** Text shown as is (numbers, units). */
    record Literal(String text) implements HudText {}

    /** A name the platform knows how to show (e.g. a target's display name), passed through untouched. */
    record Name(Object source) implements HudText {}

    static HudText translated(String key, HudText... args) {
        return new Translated(key, args);
    }

    static HudText literal(Object value) {
        return new Literal(String.valueOf(value));
    }
}
