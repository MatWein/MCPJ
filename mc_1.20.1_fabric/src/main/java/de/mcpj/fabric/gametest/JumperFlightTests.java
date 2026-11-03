package de.mcpj.fabric.gametest;

import de.mcpj.mc.gametest.JumperFlightTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperFlightTestCases (see there). */
public class JumperFlightTests {
    @GameTest(template = "mcpj:platform", timeoutTicks = 100)
    public static void hoversWithoutPilot(GameTestHelper helper) {
        JumperFlightTestCases.hoversWithoutPilot(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void solidEvenWhenCloaked(GameTestHelper helper) {
        JumperFlightTestCases.solidEvenWhenCloaked(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void segmentsFollowRotation(GameTestHelper helper) {
        JumperFlightTestCases.segmentsFollowRotation(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 100)
    public static void ignoresKnockback(GameTestHelper helper) {
        JumperFlightTestCases.ignoresKnockback(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 100)
    public static void crashesWithoutEnergy(GameTestHelper helper) {
        JumperFlightTestCases.crashesWithoutEnergy(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void impactDamageCurve(GameTestHelper helper) {
        JumperFlightTestCases.impactDamageCurve(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 100)
    public static void edgeGuidanceSlipsThroughOpening(GameTestHelper helper) {
        JumperFlightTestCases.edgeGuidanceSlipsThroughOpening(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 100)
    public static void edgeGuidanceIgnoresWideMiss(GameTestHelper helper) {
        JumperFlightTestCases.edgeGuidanceIgnoresWideMiss(helper);
    }
}
