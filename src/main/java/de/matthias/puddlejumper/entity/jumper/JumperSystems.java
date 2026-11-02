package de.matthias.puddlejumper.entity.jumper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

public final class JumperSystems {
    private static final double CLOAK_FORGET_RANGE = 48;
    private static final String CLOAKED_TAG = "Cloaked";
    private static final String SHIELDED_TAG = "Shielded";
    private static final String LIGHTS_TAG = "Lights";

    private final PuddleJumperEntity jumper;

    JumperSystems(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    public void perform(ServerPlayer pilot, JumperAction action, int targetId) {
        if (jumper.getControllingPassenger() != pilot) return;
        if (switchesOn(action) && !jumper.energy().hasEnergy()) {
            JumperSounds.denied(jumper);
            return;
        }
        switch (action) {
            case CLOAK -> setCloak(!jumper.isCloaked());
            case SHIELD -> setShield(!jumper.isShielded());
            case LIGHTS -> setLights(!jumper.isLightsOn());
            case PODS -> jumper.pods().toggle();
            case FIRE -> jumper.weapons().fire(pilot, targetId);
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
        jumper.setCloaked(on);
        JumperSounds.cloakToggled(jumper, on);
        if (on) loseMobPursuers();
    }

    private void setShield(boolean on) {
        jumper.setShielded(on);
        JumperSounds.shieldToggled(jumper, on);
    }

    private void setLights(boolean on) {
        jumper.setLightsOn(on);
        JumperSounds.lightsToggled(jumper, on);
    }

    private void loseMobPursuers() {
        for (Mob mob : jumper.level().getEntitiesOfClass(Mob.class, jumper.getBoundingBox().inflate(CLOAK_FORGET_RANGE))) {
            if (mob.getTarget() != null && jumper.hasPassenger(mob.getTarget())) mob.setTarget(null);
        }
    }

    void collapseShield() {
        jumper.setShielded(false);
        JumperSounds.shieldCollapsed(jumper);
        if (!jumper.energy().hasEnergy()) powerFailure();
    }

    void powerFailure() {
        if (jumper.isShielded() || jumper.isCloaked() || jumper.isLightsOn()) JumperSounds.shieldCollapsed(jumper);
        JumperSounds.powerFailure(jumper);
        jumper.setShielded(false);
        jumper.setCloaked(false);
        jumper.setLightsOn(false);
    }

    void denied() {
        JumperSounds.denied(jumper);
    }

    void load(CompoundTag tag) {
        jumper.setCloaked(tag.getBoolean(CLOAKED_TAG));
        jumper.setShielded(tag.getBoolean(SHIELDED_TAG));
        jumper.setLightsOn(tag.getBoolean(LIGHTS_TAG));
    }

    void save(CompoundTag tag) {
        tag.putBoolean(CLOAKED_TAG, jumper.isCloaked());
        tag.putBoolean(SHIELDED_TAG, jumper.isShielded());
        tag.putBoolean(LIGHTS_TAG, jumper.isLightsOn());
    }
}
