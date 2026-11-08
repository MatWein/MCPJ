package de.mcpj.core.targeting;

/** How a target is marked: hostile (red), another jumper (cyan), a jumper the player has the key to (yellow) or anything else (white). */
public enum TargetKind {
    NEUTRAL,
    ENEMY,
    JUMPER,
    KEYED_JUMPER
}
