package de.mcpj.fabric.gametest;

import de.mcpj.mc.gametest.JumperTravelTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperTravelTestCases (see there). */
public class JumperTravelTests {
    @GameTest(template = "mcpj:platform")
    public static void jumperChangesDimensionWithItsState(GameTestHelper helper) {
        JumperTravelTestCases.jumperChangesDimensionWithItsState(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void hullSegmentsNeverTravelAlone(GameTestHelper helper) {
        JumperTravelTestCases.hullSegmentsNeverTravelAlone(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void seatedPilotDoesNotTravelAlone(GameTestHelper helper) {
        JumperTravelTestCases.seatedPilotDoesNotTravelAlone(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void teleportWithinDimensionKeepsPilot(GameTestHelper helper) {
        JumperTravelTestCases.teleportWithinDimensionKeepsPilot(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void destroyedSegmentDestroysShip(GameTestHelper helper) {
        JumperTravelTestCases.destroyedSegmentDestroysShip(helper);
    }
}
