package de.mcpj.mc.entity.jumper;

/** A hull segment (bow or stern) of a jumper; each loader implements segments its own way. */
public interface JumperSegment {
    PuddleJumperEntity jumper();
}
