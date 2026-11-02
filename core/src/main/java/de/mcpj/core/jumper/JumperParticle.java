package de.mcpj.core.jumper;

/** Particle effects by meaning; each platform maps them to concrete game particles. */
public enum JumperParticle {
    /** Water leaking into the hull. */
    LEAK_BUBBLE,
    /** Hull burning in lava. */
    LAVA_SMOKE,
    /** Ion exhaust behind the drive pods. */
    ION,
    /** Occasional spark in the exhaust. */
    SPARK
}
