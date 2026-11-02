package de.mcpj.fabric.gametest;

import de.mcpj.mc.gametest.JumperEnergyTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperEnergyTestCases (see there). */
public class JumperEnergyTests {
    @GameTest(template = "mcpj:platform")
    public static void startChargeAndNoDriveDrainWhenLanded(GameTestHelper helper) {
        JumperEnergyTestCases.startChargeAndNoDriveDrainWhenLanded(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 100)
    public static void systemsDrainEnergy(GameTestHelper helper) {
        JumperEnergyTestCases.systemsDrainEnergy(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void shieldCollapsesWithoutEnergy(GameTestHelper helper) {
        JumperEnergyTestCases.shieldCollapsesWithoutEnergy(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 60)
    public static void chargingPadCharges(GameTestHelper helper) {
        JumperEnergyTestCases.chargingPadCharges(helper);
    }
}
