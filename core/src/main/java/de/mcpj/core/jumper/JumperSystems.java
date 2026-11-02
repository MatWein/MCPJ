package de.mcpj.core.jumper;

import de.mcpj.core.math.Vec3d;

/** Cloak, shield, lights and the pilot's actions; everything fails without energy. */
public final class JumperSystems {
    private static final double CLOAK_FORGET_RANGE = 48;
    private static final String CLOAKED_TAG = "Cloaked";
    private static final String SHIELDED_TAG = "Shielded";
    private static final String LIGHTS_TAG = "Lights";

    private final Jumper jumper;

    JumperSystems(Jumper jumper) {
        this.jumper = jumper;
    }

    /** A pilot action; the platform has already checked that it comes from the pilot. */
    public void perform(JumperAction action, Vec3d pilotLook, int targetId) {
        if (switchesOn(action) && !jumper.energy().hasEnergy()) {
            JumperSounds.denied(jumper);
            return;
        }
        switch (action) {
            case CLOAK -> setCloak(!jumper.isCloaked());
            case SHIELD -> setShield(!jumper.isShielded());
            case LIGHTS -> setLights(!jumper.isLightsOn());
            case PODS -> jumper.pods().toggle();
            case FIRE -> jumper.weapons().fire(pilotLook, targetId);
        }
    }

    private boolean switchesOn(JumperAction action) {
        return switch (action) {
            case CLOAK -> !jumper.isCloaked();
            case SHIELD -> !jumper.isShielded();
            case LIGHTS -> !jumper.isLightsOn();
            case PODS, FIRE -> false;
        };
    }

    void setCloak(boolean on) {
        jumper.sync().setCloaked(on);
        JumperSounds.cloakToggled(jumper, on);
        if (on) jumper.host().loseMobPursuers(CLOAK_FORGET_RANGE);
    }

    private void setShield(boolean on) {
        jumper.sync().setShielded(on);
        JumperSounds.shieldToggled(jumper, on);
    }

    private void setLights(boolean on) {
        jumper.sync().setLightsOn(on);
        JumperSounds.lightsToggled(jumper, on);
    }

    void collapseShield() {
        jumper.sync().setShielded(false);
        JumperSounds.shieldCollapsed(jumper);
        if (!jumper.energy().hasEnergy()) powerFailure();
    }

    void powerFailure() {
        if (jumper.isShielded() || jumper.isCloaked() || jumper.isLightsOn()) JumperSounds.shieldCollapsed(jumper);
        JumperSounds.powerFailure(jumper);
        jumper.sync().setShielded(false);
        jumper.sync().setCloaked(false);
        jumper.sync().setLightsOn(false);
    }

    void denied() {
        JumperSounds.denied(jumper);
    }

    public void load(SaveData data) {
        jumper.sync().setCloaked(data.getBoolean(CLOAKED_TAG));
        jumper.sync().setShielded(data.getBoolean(SHIELDED_TAG));
        jumper.sync().setLightsOn(data.getBoolean(LIGHTS_TAG));
    }

    public void save(SaveData data) {
        data.putBoolean(CLOAKED_TAG, jumper.isCloaked());
        data.putBoolean(SHIELDED_TAG, jumper.isShielded());
        data.putBoolean(LIGHTS_TAG, jumper.isLightsOn());
    }
}
