package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperHazardTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperHazardTestCases (see there). */
@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperHazardTests {
    @GameTest(template = "platform", timeoutTicks = 120)
    public static void lavaDamagesHull(GameTestHelper helper) {
        JumperHazardTestCases.lavaDamagesHull(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 300)
    public static void flowingLavaFromAboveDamagesHull(GameTestHelper helper) {
        JumperHazardTestCases.flowingLavaFromAboveDamagesHull(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 120)
    public static void shieldAbsorbsLava(GameTestHelper helper) {
        JumperHazardTestCases.shieldAbsorbsLava(helper);
    }

    @GameTest(template = "platform")
    public static void passengerIsInvulnerable(GameTestHelper helper) {
        JumperHazardTestCases.passengerIsInvulnerable(helper);
    }

    @GameTest(template = "platform")
    public static void destructionEjectsPilotAndExplodes(GameTestHelper helper) {
        JumperHazardTestCases.destructionEjectsPilotAndExplodes(helper);
    }
}
