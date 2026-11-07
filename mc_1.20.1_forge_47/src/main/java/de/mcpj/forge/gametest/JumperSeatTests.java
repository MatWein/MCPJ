package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import de.mcpj.mc.gametest.JumperSeatTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperSeatTestCases (see there). */
@GameTestHolder(McpjMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperSeatTests {
    @GameTest(template = "platform")
    public static void seatsFillUpToCapacity(GameTestHelper helper) {
        JumperSeatTestCases.seatsFillUpToCapacity(helper);
    }

    @GameTest(template = "platform")
    public static void nextPassengerTakesOver(GameTestHelper helper) {
        JumperSeatTestCases.nextPassengerTakesOver(helper);
    }

    @GameTest(template = "platform")
    public static void passengersAreProtectedLikeThePilot(GameTestHelper helper) {
        JumperSeatTestCases.passengersAreProtectedLikeThePilot(helper);
    }
}
