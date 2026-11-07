package de.mcpj.core.projector;

import de.mcpj.core.config.DefaultConfigBuilder;
import de.mcpj.core.config.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumperProjectorTest {
    private static class Host implements ProjectorHost {
        boolean spaceFree = true;
        boolean jumper;
        int started;
        boolean finished;
        boolean building;
        float shownProgress;
        boolean shownPowered;

        @Override public boolean isSpaceFree() { return spaceFree; }
        @Override public boolean startJumper() { started++; jumper = true; return true; }
        @Override public boolean hasJumper() { return jumper; }
        @Override public void updateJumper(float progress, boolean powered) { shownProgress = progress; shownPowered = powered; }
        @Override public void finishJumper() { finished = true; }
        @Override public void setBuilding(boolean building) { this.building = building; }
        @Override public void markChanged() {}
    }

    private final Host host = new Host();
    private JumperProjector projector;

    @BeforeEach
    void setUp() {
        DefaultConfigBuilder.useDefaultsExcept(Map.of("buildSeconds", 1));
        projector = new JumperProjector(host);
    }

    private void run(boolean redstone, int ticks) {
        for (int i = 0; i < ticks; i++) projector.serverTick(redstone);
    }

    @Test
    void onePulseBuildsOneJumper() {
        run(false, 3);
        assertEquals(0, host.started, "Started without a pulse");
        run(true, 5);
        assertEquals(1, host.started);
        assertTrue(host.building);
        assertTrue(host.shownPowered, "Jumper hidden although no energy is required");
        run(false, 2);
        run(true, 2);
        assertEquals(1, host.started, "A pulse while building started another jumper");
        run(false, 20);
        assertTrue(host.finished);
        assertFalse(host.building);
        assertFalse(projector.isBuilding());
        // the signal stayed on after finishing: no new build without a new pulse
        run(true, 1);
        assertEquals(2, host.started, "A new pulse after finishing should start the next jumper");
    }

    @Test
    void noStartWithoutSpaceAndPauseWhileBlocked() {
        host.spaceFree = false;
        run(true, 1);
        run(false, 1);
        assertEquals(0, host.started);
        host.spaceFree = true;
        run(true, 1);
        run(false, 5);
        float progress = projector.progressFraction();
        host.spaceFree = false;
        run(false, 10);
        assertEquals(progress, projector.progressFraction(), "Build went on while the space was blocked");
        assertTrue(projector.isBuilding());
    }

    @Test
    void needsEnergyOnlyIfRequired() {
        DefaultConfigBuilder.useDefaultsExcept(Map.of("buildSeconds", 1, "requirePower", true, "energyPerJumper", 2_000));
        projector.serverTick(true);
        run(false, 5);
        assertEquals(0f, projector.progressFraction(), "Built without energy");
        assertFalse(host.shownPowered, "Jumper shown without energy");
        projector.receive(JumperProjector.energyPerTick() * 3, false);
        projector.serverTick(false);
        assertTrue(host.shownPowered, "Jumper hidden although energy is available");
        run(false, 4);
        assertEquals(3f / ServerConfig.PROJECTOR.buildTicks(), projector.progressFraction(), 1e-6);
        assertFalse(host.shownPowered, "Jumper still shown after the energy ran out");
    }

    @Test
    void playerStartsWithoutRedstone() {
        assertTrue(projector.start());
        assertEquals(1, host.started);
        assertTrue(projector.isBuilding());
        assertFalse(projector.start(), "Started a second build while building");
        assertEquals(1, host.started);
    }

    @Test
    void playerStartNeedsSpace() {
        host.spaceFree = false;
        assertFalse(projector.start());
        assertEquals(0, host.started);
    }

    @Test
    void givesUpWhenTheJumperIsGone() {
        projector.serverTick(true);
        host.jumper = false;
        run(false, 50);
        assertTrue(projector.isBuilding(), "Gave up before the grace time (jumper may load later)");
        run(false, 100);
        assertFalse(projector.isBuilding());
        assertFalse(host.building);
    }
}
