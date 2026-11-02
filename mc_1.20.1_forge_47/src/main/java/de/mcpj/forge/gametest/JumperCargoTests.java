package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperCargoTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperCargoTestCases (see there). */
@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperCargoTests {
    @GameTest(template = "platform")
    public static void cargoSurvivesSave(GameTestHelper helper) {
        JumperCargoTestCases.cargoSurvivesSave(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 60)
    public static void picksUpNearbyItems(GameTestHelper helper) {
        JumperCargoTestCases.picksUpNearbyItems(helper);
    }
}
