package de.mcpj.fabric.gametest;

import de.mcpj.mc.gametest.JumperHazardTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperHazardTestCases (see there). */
public class JumperHazardTests {
    @GameTest(template = "mcpj:platform", timeoutTicks = 120)
    public static void lavaDamagesHull(GameTestHelper helper) {
        JumperHazardTestCases.lavaDamagesHull(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 300)
    public static void flowingLavaFromAboveDamagesHull(GameTestHelper helper) {
        JumperHazardTestCases.flowingLavaFromAboveDamagesHull(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 120)
    public static void shieldAbsorbsLava(GameTestHelper helper) {
        JumperHazardTestCases.shieldAbsorbsLava(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void passengerIsInvulnerable(GameTestHelper helper) {
        JumperHazardTestCases.passengerIsInvulnerable(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void destructionEjectsPilotAndExplodes(GameTestHelper helper) {
        JumperHazardTestCases.destructionEjectsPilotAndExplodes(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void passengersKeepTheirAir(GameTestHelper helper) {
        JumperHazardTestCases.passengersKeepTheirAir(helper);
    }
}
