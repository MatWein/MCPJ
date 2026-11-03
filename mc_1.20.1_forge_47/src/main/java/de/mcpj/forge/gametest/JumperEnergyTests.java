package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperEnergyTestCases;
import de.mcpj.mc.gametest.JumperProjectorTestCases;
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

    @GameTest(template = "platform", timeoutTicks = 60)
    public static void chargingPadChargesWithoutPower(GameTestHelper helper) {
        JumperEnergyTestCases.chargingPadChargesWithoutPower(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 60)
    public static void projectorBuildsJumperAfterPulse(GameTestHelper helper) {
        JumperProjectorTestCases.projectorBuildsJumperAfterPulse(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 40)
    public static void projectorNeedsSpace(GameTestHelper helper) {
        JumperProjectorTestCases.projectorNeedsSpace(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 60)
    public static void unfinishedJumperVanishesWithProjector(GameTestHelper helper) {
        JumperProjectorTestCases.unfinishedJumperVanishesWithProjector(helper);
    }
}
