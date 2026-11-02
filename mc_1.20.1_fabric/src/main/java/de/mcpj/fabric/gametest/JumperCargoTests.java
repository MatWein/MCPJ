package de.mcpj.fabric.gametest;

import de.mcpj.mc.gametest.JumperCargoTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperCargoTestCases (see there). */
public class JumperCargoTests {
    @GameTest(template = "mcpj:platform")
    public static void cargoSurvivesSave(GameTestHelper helper) {
        JumperCargoTestCases.cargoSurvivesSave(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 60)
    public static void picksUpNearbyItems(GameTestHelper helper) {
        JumperCargoTestCases.picksUpNearbyItems(helper);
    }
}
