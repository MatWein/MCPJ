package de.mcpj.mc.entity.jumper;

import de.mcpj.mc.compat.McCompat;
import de.mcpj.core.jumper.JumperParticle;
import de.mcpj.core.jumper.JumperSound;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Minecraft sounds and particles for the core's ship effects. */
final class JumperEffects {
    private static final DustParticleOptions ION = McCompat.dust(0x73BFFF, 0.6f);

    private JumperEffects() {}

    static SoundEvent sound(JumperSound sound) {
        return switch (sound) {
            case IMPACT_BLAST -> McCompat.explodeSound();
            case IMPACT_CLANG, HULL_HIT -> SoundEvents.ANVIL_LAND;
            case SHIELD_HIT -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case LAVA_BURN -> SoundEvents.LAVA_EXTINGUISH;
            // conduit: a shimmering, watery energy hum that matches the bubble's water look
            case SHIELD_ON -> SoundEvents.CONDUIT_ACTIVATE;
            case SHIELD_OFF, POWER_FAILURE -> SoundEvents.CONDUIT_DEACTIVATE;
            case SHIELD_COLLAPSE -> SoundEvents.BEACON_DEACTIVATE;
            // illusioner vanishing into its mirror images
            case CLOAK -> SoundEvents.ILLUSIONER_MIRROR_MOVE;
            case LIGHTS -> SoundEvents.LEVER_CLICK;
            case PODS_OUT -> SoundEvents.IRON_DOOR_OPEN;
            case PODS_IN -> SoundEvents.IRON_DOOR_CLOSE;
            case DRONE_LAUNCH -> SoundEvents.FIRECHARGE_USE;
            case DRONE_LAUNCH_CHIME -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case DENIED -> SoundEvents.DISPENSER_FAIL;
        };
    }

    static ParticleOptions particle(JumperParticle particle) {
        return switch (particle) {
            case LEAK_BUBBLE -> ParticleTypes.BUBBLE;
            case LAVA_SMOKE -> ParticleTypes.LARGE_SMOKE;
            case ION -> ION;
            case SPARK -> ParticleTypes.ELECTRIC_SPARK;
        };
    }
}
