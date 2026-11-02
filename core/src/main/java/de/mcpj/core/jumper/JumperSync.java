package de.mcpj.core.jumper;

import de.mcpj.core.math.Vec3d;

/** State the server shares with all clients (the platform syncs it, e.g. via entity data). */
public interface JumperSync {
    boolean cloaked();

    void setCloaked(boolean cloaked);

    boolean shielded();

    void setShielded(boolean shielded);

    boolean lightsOn();

    void setLightsOn(boolean lightsOn);

    boolean podsOut();

    void setPodsOut(boolean podsOut);

    int hurtTime();

    void setHurtTime(int ticks);

    int hurtDirection();

    void setHurtDirection(int direction);

    float hurtWobble();

    void setHurtWobble(float wobble);

    float hullIntegrity();

    void setHullIntegrity(float integrity);

    int storedEnergy();

    void setStoredEnergy(int energy);

    int capacity();

    void setCapacity(int capacity);

    int energyFlow();

    void setEnergyFlow(int flow);

    Vec3d arrivalVelocity();

    int arrivals();

    /** Announces an arrival through a portal with the velocity to continue with. */
    void announceArrival(Vec3d velocity);
}
