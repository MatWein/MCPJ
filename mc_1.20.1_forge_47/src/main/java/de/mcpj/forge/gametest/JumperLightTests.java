package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperLightTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperLightTestCases (see there). */
@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperLightTests {
    @GameTest(template = "platform")
    public static void lightsPlaceAndRemoveBlocks(GameTestHelper helper) {
        JumperLightTestCases.lightsPlaceAndRemoveBlocks(helper);
    }

    @GameTest(template = "platform", timeoutTicks = 60)
    public static void lightsFollowLazily(GameTestHelper helper) {
        JumperLightTestCases.lightsFollowLazily(helper);
    }
}
