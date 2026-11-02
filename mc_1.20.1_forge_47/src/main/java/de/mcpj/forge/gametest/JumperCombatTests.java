package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperCombatTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperCombatTestCases (see there). */
@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperCombatTests {
    @GameTest(template = "platform", timeoutTicks = 300)
    public static void droneHitsZombie(GameTestHelper helper) {
        JumperCombatTestCases.droneHitsZombie(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 300)
    public static void droneHitsOtherJumper(GameTestHelper helper) {
        JumperCombatTestCases.droneHitsOtherJumper(helper);
    }

    @GameTest(template = "platform")
    public static void ownDroneExplosionSparesJumper(GameTestHelper helper) {
        JumperCombatTestCases.ownDroneExplosionSparesJumper(helper);
    }

    @GameTest(template = "platform")
    public static void shieldMakesIndestructible(GameTestHelper helper) {
        JumperCombatTestCases.shieldMakesIndestructible(helper);
    }

    @GameTest(template = "platform")
    public static void survivesOneDroneNotTwo(GameTestHelper helper) {
        JumperCombatTestCases.survivesOneDroneNotTwo(helper);
    }

    @GameTest(template = "platform")
    public static void breaksAndDropsCargo(GameTestHelper helper) {
        JumperCombatTestCases.breaksAndDropsCargo(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 120)
    public static void waterDamagesHull(GameTestHelper helper) {
        JumperCombatTestCases.waterDamagesHull(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 120)
    public static void shieldBlocksWater(GameTestHelper helper) {
        JumperCombatTestCases.shieldBlocksWater(helper);
    }
}
