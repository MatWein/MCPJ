package de.mcpj.fabric.gametest;

import de.mcpj.mc.gametest.JumperLightTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperLightTestCases (see there). */
public class JumperLightTests {
    @GameTest(template = "mcpj:platform")
    public static void lightsPlaceAndRemoveBlocks(GameTestHelper helper) {
        JumperLightTestCases.lightsPlaceAndRemoveBlocks(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 60)
    public static void lightsFollowLazily(GameTestHelper helper) {
        JumperLightTestCases.lightsFollowLazily(helper);
    }
}
