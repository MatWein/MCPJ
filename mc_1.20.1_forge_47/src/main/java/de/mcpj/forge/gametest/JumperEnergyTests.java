package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperEnergyTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperEnergyTestCases (see there). */
@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperEnergyTests {
    @GameTest(template = "platform")
    public static void startChargeAndNoDriveDrainWhenLanded(GameTestHelper helper) {
        JumperEnergyTestCases.startChargeAndNoDriveDrainWhenLanded(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 100)
    public static void systemsDrainEnergy(GameTestHelper helper) {
        JumperEnergyTestCases.systemsDrainEnergy(helper);
    }

    @GameTest(template = "platform")
    public static void shieldCollapsesWithoutEnergy(GameTestHelper helper) {
        JumperEnergyTestCases.shieldCollapsesWithoutEnergy(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 60)
    public static void chargingPadCharges(GameTestHelper helper) {
        JumperEnergyTestCases.chargingPadCharges(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 60)
    public static void lightningChargesJumper(GameTestHelper helper) {
        JumperEnergyTestCases.lightningChargesJumper(helper);
    }

    @GameTest(template = "platform")
    public static void jumperAttractsLightning(GameTestHelper helper) {
        JumperEnergyTestCases.jumperAttractsLightning(helper);
    }
}
