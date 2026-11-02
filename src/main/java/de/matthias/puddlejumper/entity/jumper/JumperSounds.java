package de.matthias.puddlejumper.entity.jumper;

import net.minecraft.sounds.SoundEvents;

final class JumperSounds {
    private JumperSounds() {}

    static void impact(PuddleJumperEntity jumper, float severity) {
        jumper.playSound(SoundEvents.GENERIC_EXPLODE, 0.5f + severity * 2f, 1.4f);
        jumper.playSound(SoundEvents.ANVIL_LAND, 0.8f, 0.5f);
    }

    static void shieldHit(PuddleJumperEntity jumper) {
        jumper.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.6f);
    }

    static void lavaBurn(PuddleJumperEntity jumper) {
        jumper.playSound(SoundEvents.LAVA_EXTINGUISH, 0.6f, 0.8f);
    }

    static void hullHit(PuddleJumperEntity jumper) {
        jumper.playSound(SoundEvents.ANVIL_LAND, 0.25f, 1.6f);
    }

    /** Conduit: a shimmering, watery energy hum that matches the bubble's water look. */
    static void shieldToggled(PuddleJumperEntity jumper, boolean on) {
        jumper.playSound(on ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE, 1.0f, on ? 1.2f : 1.4f);
    }

    static void shieldCollapsed(PuddleJumperEntity jumper) {
        jumper.playSound(SoundEvents.BEACON_DEACTIVATE, 1.2f, 0.8f);
    }

    /** Illusioner vanishing into its mirror images: a magical whoosh, higher when vanishing, lower when reappearing. */
    static void cloakToggled(PuddleJumperEntity jumper, boolean on) {
        jumper.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, on ? 1.2f : 0.8f);
    }

    static void lightsToggled(PuddleJumperEntity jumper, boolean on) {
        jumper.playSound(SoundEvents.LEVER_CLICK, 0.6f, on ? 1.2f : 0.9f);
    }

    static void podsToggled(PuddleJumperEntity jumper, boolean out) {
        jumper.playSound(out ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 0.8f, 0.7f);
    }

    static void droneLaunched(PuddleJumperEntity jumper) {
        jumper.playSound(SoundEvents.FIRECHARGE_USE, 0.8f, 1.8f);
        jumper.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
    }

    static void powerFailure(PuddleJumperEntity jumper) {
        jumper.playSound(SoundEvents.CONDUIT_DEACTIVATE, 1.5f, 0.7f);
    }

    static void denied(PuddleJumperEntity jumper) {
        jumper.playSound(SoundEvents.DISPENSER_FAIL, 0.8f, 1.2f);
    }
}
