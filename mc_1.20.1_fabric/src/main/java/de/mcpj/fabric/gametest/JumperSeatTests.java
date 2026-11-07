package de.mcpj.fabric.gametest;

import de.mcpj.mc.gametest.JumperSeatTestCases;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Registers the shared JumperSeatTestCases (see there). */
public class JumperSeatTests {
    @GameTest(template = "mcpj:platform")
    public static void seatsFillUpToCapacity(GameTestHelper helper) {
        JumperSeatTestCases.seatsFillUpToCapacity(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void nextPassengerTakesOver(GameTestHelper helper) {
        JumperSeatTestCases.nextPassengerTakesOver(helper);
    }

    @GameTest(template = "mcpj:platform")
    public static void passengersAreProtectedLikeThePilot(GameTestHelper helper) {
        JumperSeatTestCases.passengersAreProtectedLikeThePilot(helper);
    }
}
