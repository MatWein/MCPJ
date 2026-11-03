package de.mcpj.fabric.gametest;

import de.mcpj.mc.gametest.JumperCombatTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperCombatTestCases (see there). */
public class JumperCombatTests {
    @GameTest(template = "mcpj:platform", timeoutTicks = 300)
    public static void droneHitsZombie(GameTestHelper helper) {
        JumperCombatTestCases.droneHitsZombie(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 300)
    public static void droneHitsOtherJumper(GameTestHelper helper) {
        JumperCombatTestCases.droneHitsOtherJumper(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void ownDroneExplosionSparesJumper(GameTestHelper helper) {
        JumperCombatTestCases.ownDroneExplosionSparesJumper(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void shieldMakesIndestructible(GameTestHelper helper) {
        JumperCombatTestCases.shieldMakesIndestructible(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void survivesOneDroneNotTwo(GameTestHelper helper) {
        JumperCombatTestCases.survivesOneDroneNotTwo(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void breaksAndDropsCargo(GameTestHelper helper) {
        JumperCombatTestCases.breaksAndDropsCargo(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 120)
    public static void waterDamagesHull(GameTestHelper helper) {
        JumperCombatTestCases.waterDamagesHull(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 120)
    public static void shieldBlocksWater(GameTestHelper helper) {
        JumperCombatTestCases.shieldBlocksWater(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void attackOnPilotHitsHull(GameTestHelper helper) {
        JumperCombatTestCases.attackOnPilotHitsHull(helper);
    }

    @GameTest(template = "mcpj:platform", timeoutTicks = 100)
    public static void zombieAttacksHullOfPilot(GameTestHelper helper) {
        JumperCombatTestCases.zombieAttacksHullOfPilot(helper);
    }
}
