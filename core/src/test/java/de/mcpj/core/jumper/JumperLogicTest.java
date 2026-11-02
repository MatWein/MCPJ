package de.mcpj.core.jumper;

import de.mcpj.core.config.DefaultConfigBuilder;
import de.mcpj.core.config.ServerConfig;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Vec3d;
import de.mcpj.core.util.Units;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ship logic without Minecraft, in an empty test world. */
class JumperLogicTest {
    private FakeJumperHost host;
    private Jumper jumper;

    @BeforeEach
    void setUp() {
        DefaultConfigBuilder.useDefaults();
        host = new FakeJumperHost();
        jumper = new Jumper(host);
        jumper.onAdded();
    }

    private void runSeconds(int seconds) {
        for (int i = 0; i < seconds * Units.TICKS_PER_SECOND; i++) host.serverTick(jumper);
    }

    private void putLavaIntoHull() {
        host.lava.add(BlockPosition.containing(host.location.add(0, 0.5, 0)));
    }

    @Test
    void newJumpersStartWithTheConfiguredCharge() {
        assertEquals(Math.round(ServerConfig.ENERGY.capacity() * ServerConfig.ENERGY.startCharge()), jumper.energy().stored());
    }

    @Test
    void lavaBurnsTheHullWithoutShield() {
        putLavaIntoHull();
        runSeconds(2);
        assertEquals(JumperMedium.LAVA, jumper.medium());
        float perSecond = ServerConfig.HULL.lavaDamagePerSecond() / ServerConfig.HULL.maxHull();
        assertEquals(1f - 2 * perSecond, jumper.hull().integrity(), 1e-5);
        assertTrue(host.sounds.contains(JumperSound.LAVA_BURN));
    }

    @Test
    void shieldAbsorbsLavaWhileEnergyLasts() {
        host.shielded = true;
        putLavaIntoHull();
        int before = jumper.energy().stored();
        runSeconds(2);
        assertEquals(1f, jumper.hull().integrity());
        assertTrue(jumper.energy().stored() < before, "shield used no energy");
        assertTrue(host.events.contains("shield hit"));
    }

    @Test
    void destroyedShipEjectsBeforeItExplodesAndDropsAfterwards() {
        jumper.hull().hurt(new JumperHull.Damage(1000f, null, false, false, null, false, false));
        List<String> order = host.events.stream()
                .filter(e -> e.equals("eject") || e.equals("discard") || e.startsWith("explode") || e.startsWith("drop"))
                .toList();
        assertEquals(List.of("eject", "discard", "explode " + ServerConfig.HULL.destructionExplosionPower(), "drop item", "drop cargo"), order);
    }

    @Test
    void creativeAttackersGetNoShipItem() {
        jumper.hull().hurt(new JumperHull.Damage(1000f, null, false, false, null, false, true));
        assertFalse(host.events.contains("drop item"));
        assertTrue(host.events.contains("drop cargo"));
    }

    @Test
    void podsFollowLandingAndCanBeOverridden() {
        host.onGround = false;
        host.serverTick(jumper);
        assertTrue(host.podsOut, "pods should be out in the air");
        jumper.pods().toggle();
        host.serverTick(jumper);
        assertFalse(host.podsOut, "manual override ignored");
        host.onGround = true;
        host.serverTick(jumper);
        assertFalse(host.podsOut, "pods should be in when landed");
    }

    @Test
    void portalKeepsSpeedAlongTheNewHeading() {
        Vec3d northwards = new Vec3d(0, 0, -1);
        host.yaw = 90;
        jumper.continueAfterTeleport(northwards, 180);
        // heading turned from north (180) to west (90): the speed now points west
        assertEquals(-1, host.arrivalVelocity.x(), 1e-6);
        assertEquals(0, host.arrivalVelocity.z(), 1e-6);
        assertEquals(1, host.arrivals);
    }

    @Test
    void withoutEnergyEverySystemFails() {
        host.shielded = true;
        host.cloaked = true;
        jumper.energy().set(1);
        host.onGround = false;
        runSeconds(1);
        assertFalse(host.shielded);
        assertFalse(host.cloaked);
        assertTrue(host.sounds.contains(JumperSound.POWER_FAILURE));
    }
}
