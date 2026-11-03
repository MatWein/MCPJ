package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperFlightTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperFlightTestCases (see there). */
@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperFlightTests {
    @GameTest(template = "platform", timeoutTicks = 100)
    public static void hoversWithoutPilot(GameTestHelper helper) {
        JumperFlightTestCases.hoversWithoutPilot(helper);
    }

    @GameTest(template = "platform")
    public static void solidEvenWhenCloaked(GameTestHelper helper) {
        JumperFlightTestCases.solidEvenWhenCloaked(helper);
    }

    @GameTest(template = "platform")
    public static void segmentsFollowRotation(GameTestHelper helper) {
        JumperFlightTestCases.segmentsFollowRotation(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void ignoresKnockback(GameTestHelper helper) {
        JumperFlightTestCases.ignoresKnockback(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void crashesWithoutEnergy(GameTestHelper helper) {
        JumperFlightTestCases.crashesWithoutEnergy(helper);
    }

    @GameTest(template = "platform")
    public static void impactDamageCurve(GameTestHelper helper) {
        JumperFlightTestCases.impactDamageCurve(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void edgeGuidanceSlipsThroughOpening(GameTestHelper helper) {
        JumperFlightTestCases.edgeGuidanceSlipsThroughOpening(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void edgeGuidanceIgnoresWideMiss(GameTestHelper helper) {
        JumperFlightTestCases.edgeGuidanceIgnoresWideMiss(helper);
    }
}
