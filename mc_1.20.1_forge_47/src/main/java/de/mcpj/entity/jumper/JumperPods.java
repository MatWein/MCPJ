package de.mcpj.entity.jumper;

import org.jetbrains.annotations.Nullable;

import static de.mcpj.entity.jumper.PuddleJumperEntity.PODS_OUT;

final class JumperPods {
    private final PuddleJumperEntity jumper;
    @Nullable private Boolean manualState;
    private boolean wasLanded;
    private int deployTimer;

    JumperPods(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    void tick() {
        if (deployTimer > 0) deployTimer--;
        boolean landed = jumper.onGround();
        if (landed != wasLanded) {
            manualState = null;
            wasLanded = landed;
        }
        boolean automatic = !landed;
        setOut(deployTimer > 0 || (manualState != null ? manualState : automatic));
    }

    void toggle() {
        manualState = !jumper.isPodsOut();
        deployTimer = 0;
        JumperSounds.podsToggled(jumper, manualState);
    }

    void deployFor(int ticks) {
        deployTimer = ticks;
        setOut(true);
    }

    private void setOut(boolean out) {
        if (out != jumper.isPodsOut()) jumper.getEntityData().set(PODS_OUT, out);
    }
}
