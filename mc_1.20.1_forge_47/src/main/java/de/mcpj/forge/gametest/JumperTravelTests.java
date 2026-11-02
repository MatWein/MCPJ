package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperTravelTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperTravelTestCases (see there). */
@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperTravelTests {
    @GameTest(template = "platform")
    public static void jumperChangesDimensionWithItsState(GameTestHelper helper) {
        JumperTravelTestCases.jumperChangesDimensionWithItsState(helper);
    }

    @GameTest(template = "platform")
    public static void hullSegmentsNeverTravelAlone(GameTestHelper helper) {
        JumperTravelTestCases.hullSegmentsNeverTravelAlone(helper);
    }

    @GameTest(template = "platform")
    public static void seatedPilotDoesNotTravelAlone(GameTestHelper helper) {
        JumperTravelTestCases.seatedPilotDoesNotTravelAlone(helper);
    }

    @GameTest(template = "platform")
    public static void teleportWithinDimensionKeepsPilot(GameTestHelper helper) {
        JumperTravelTestCases.teleportWithinDimensionKeepsPilot(helper);
    }

    @GameTest(template = "platform")
    public static void destroyedSegmentDestroysShip(GameTestHelper helper) {
        JumperTravelTestCases.destroyedSegmentDestroysShip(helper);
    }
}
