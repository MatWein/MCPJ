package de.mcpj.core.jumper;

import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Box;
import de.mcpj.core.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

/** A ship in an empty test world: no blocks, records sounds and actions. */
class FakeJumperHost implements JumperHost, JumperSync {
    Vec3d location = new Vec3d(0, 64, 0);
    Vec3d previousLocation = location;
    float yaw;
    float previousYaw;
    Vec3d velocity = Vec3d.ZERO;
    boolean onGround;
    boolean clientSide;
    boolean pilot;
    boolean alive = true;
    int tickCount;
    long gameTime;
    double waterHeight;
    /** Energy every pad block below the hull hands over per call (0 = no pads). */
    int padSupply;
    final Set<BlockPosition> lava = new HashSet<>();
    final List<String> events = new ArrayList<>();
    final List<JumperSound> sounds = new ArrayList<>();
    final Random random = new Random(1);

    boolean cloaked, shielded, lightsOn, podsOut;
    int hurtTime, hurtDirection = 1, energy, capacity = 1, energyFlow, arrivals;
    float hurtWobble, hullIntegrity = 1f;
    Vec3d arrivalVelocity = Vec3d.ZERO;

    /** Advances the world by one server tick in the same order as the platform does. */
    void serverTick(Jumper jumper) {
        tickCount++;
        gameTime++;
        jumper.beforeTick();
        jumper.flightTick();
        jumper.trackServerVelocity();
        jumper.serverTick();
    }

    // body
    @Override public Vec3d location() { return location; }
    @Override public Vec3d previousLocation() { return previousLocation; }
    @Override public float yaw() { return yaw; }
    @Override public float previousYaw() { return previousYaw; }
    @Override public void setYaw(float yaw) { this.yaw = yaw; }
    @Override public Vec3d velocity() { return velocity; }
    @Override public void setVelocity(Vec3d velocity) { this.velocity = velocity; }
    @Override public boolean onGround() { return onGround; }
    @Override public boolean isClientSide() { return clientSide; }
    @Override public boolean isControlledLocally() { return !clientSide; }
    @Override public boolean hasPilot() { return pilot; }
    @Override public boolean hasPassengers() { return pilot; }
    @Override public boolean isAlive() { return alive; }
    @Override public int tickCount() { return tickCount; }
    @Override public long gameTime() { return gameTime; }
    @Override public RandomGenerator random() { return random; }
    @Override public JumperSync sync() { return this; }

    // world: empty, nothing collides
    @Override public Vec3d collide(Vec3d wanted, Box segment) { return wanted; }
    @Override public Vec3d moveBy(Vec3d movement) { location = location.add(movement); return movement; }
    @Override public boolean isFree(Box box) { return !onGround; }
    @Override public double waterHeight() { return waterHeight; }
    @Override public double lavaHeight(BlockPosition pos) { return lava.contains(pos) ? 1.0 : -1; }
    @Override public int chargeFromPad(BlockPosition pos, int room, int intervalTicks, float chargeFraction) { return Math.min(room, padSupply); }
    @Override public boolean placeLight(BlockPosition pos, int level) { events.add("light+" + pos); return true; }
    @Override public void removeLight(BlockPosition pos) { events.add("light-" + pos); }

    // effects
    @Override public void playSound(JumperSound sound, float volume, float pitch) { sounds.add(sound); }
    @Override public void serverParticles(JumperParticle particle, Vec3d at, int count, Vec3d spread, double speed) { events.add("particles " + particle); }
    @Override public void clientParticle(JumperParticle particle, Vec3d at, Vec3d velocity) {}
    @Override public void showShieldHit() { events.add("shield hit"); }
    @Override public void hullDamaged(@Nullable Object attacker) {}
    @Override public void loseMobPursuers(double range) { events.add("lose pursuers"); }
    @Override public void launchDrone(Vec3d spawn, Vec3d velocity, int targetId) { events.add("drone"); }
    @Override public void reportImpact(float horizontalSpeed, float fallSpeed) { events.add("impact report"); }
    @Override public void ejectPassengers() { events.add("eject"); pilot = false; }
    @Override public void discard() { events.add("discard"); alive = false; }
    @Override public void explode(Vec3d at, float power) { events.add("explode " + power); }
    @Override public boolean entityDropsEnabled() { return true; }
    @Override public void dropShipItem() { events.add("drop item"); }
    @Override public void dropCargo() { events.add("drop cargo"); }

    // sync
    @Override public boolean cloaked() { return cloaked; }
    @Override public void setCloaked(boolean cloaked) { this.cloaked = cloaked; }
    @Override public boolean shielded() { return shielded; }
    @Override public void setShielded(boolean shielded) { this.shielded = shielded; }
    @Override public boolean lightsOn() { return lightsOn; }
    @Override public void setLightsOn(boolean lightsOn) { this.lightsOn = lightsOn; }
    @Override public boolean podsOut() { return podsOut; }
    @Override public void setPodsOut(boolean podsOut) { this.podsOut = podsOut; }
    @Override public int hurtTime() { return hurtTime; }
    @Override public void setHurtTime(int ticks) { hurtTime = ticks; }
    @Override public int hurtDirection() { return hurtDirection; }
    @Override public void setHurtDirection(int direction) { hurtDirection = direction; }
    @Override public float hurtWobble() { return hurtWobble; }
    @Override public void setHurtWobble(float wobble) { hurtWobble = wobble; }
    @Override public float hullIntegrity() { return hullIntegrity; }
    @Override public void setHullIntegrity(float integrity) { hullIntegrity = integrity; }
    @Override public int storedEnergy() { return energy; }
    @Override public void setStoredEnergy(int energy) { this.energy = energy; }
    @Override public int capacity() { return capacity; }
    @Override public void setCapacity(int capacity) { this.capacity = capacity; }
    @Override public int energyFlow() { return energyFlow; }
    @Override public void setEnergyFlow(int flow) { energyFlow = flow; }
    @Override public Vec3d arrivalVelocity() { return arrivalVelocity; }
    @Override public int arrivals() { return arrivals; }
    @Override public void announceArrival(Vec3d velocity) { arrivalVelocity = velocity; arrivals++; }
}
