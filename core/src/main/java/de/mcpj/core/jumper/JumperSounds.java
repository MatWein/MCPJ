package de.mcpj.core.jumper;

/** Volume and pitch of the ship's sounds; which game sound plays is up to the platform. */
final class JumperSounds {
    private JumperSounds() {}

    static void impact(Jumper jumper, float severity) {
        play(jumper, JumperSound.IMPACT_BLAST, 0.5f + severity * 2f, 1.4f);
        play(jumper, JumperSound.IMPACT_CLANG, 0.8f, 0.5f);
    }

    static void shieldHit(Jumper jumper) {
        play(jumper, JumperSound.SHIELD_HIT, 0.8f, 1.6f);
    }

    static void lavaBurn(Jumper jumper) {
        play(jumper, JumperSound.LAVA_BURN, 0.6f, 0.8f);
    }

    static void hullHit(Jumper jumper) {
        play(jumper, JumperSound.HULL_HIT, 0.25f, 1.6f);
    }

    static void shieldToggled(Jumper jumper, boolean on) {
        play(jumper, on ? JumperSound.SHIELD_ON : JumperSound.SHIELD_OFF, 1.0f, on ? 1.2f : 1.4f);
    }

    static void shieldCollapsed(Jumper jumper) {
        play(jumper, JumperSound.SHIELD_COLLAPSE, 1.2f, 0.8f);
    }

    static void cloakToggled(Jumper jumper, boolean on) {
        play(jumper, JumperSound.CLOAK, 1.0f, on ? 1.2f : 0.8f);
    }

    static void lightsToggled(Jumper jumper, boolean on) {
        play(jumper, JumperSound.LIGHTS, 0.6f, on ? 1.2f : 0.9f);
    }

    static void podsToggled(Jumper jumper, boolean out) {
        play(jumper, out ? JumperSound.PODS_OUT : JumperSound.PODS_IN, 0.8f, 0.7f);
    }

    static void droneLaunched(Jumper jumper) {
        play(jumper, JumperSound.DRONE_LAUNCH, 0.8f, 1.8f);
        play(jumper, JumperSound.DRONE_LAUNCH_CHIME, 1.5f, 0.6f);
    }

    static void powerFailure(Jumper jumper) {
        play(jumper, JumperSound.POWER_FAILURE, 1.5f, 0.7f);
    }

    static void denied(Jumper jumper) {
        play(jumper, JumperSound.DENIED, 0.8f, 1.2f);
    }

    private static void play(Jumper jumper, JumperSound sound, float volume, float pitch) {
        jumper.host().playSound(sound, volume, pitch);
    }
}
