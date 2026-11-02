package de.mcpj.core.jumper;

import org.jetbrains.annotations.Nullable;

/** Drive pods: retracted when landed, extended in the air, always extended when firing; G overrides. */
public final class JumperPods {
    private final Jumper jumper;
    @Nullable private Boolean manualState;
    private boolean wasLanded;
    private int deployTimer;

    JumperPods(Jumper jumper) {
        this.jumper = jumper;
    }

    void tick() {
        if (deployTimer > 0) deployTimer--;
        boolean landed = jumper.host().onGround();
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
        if (out != jumper.isPodsOut()) jumper.sync().setPodsOut(out);
    }
}
