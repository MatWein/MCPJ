package de.mcpj.mc.gametest;

import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.Collection;

/** Minecraft up to 1.21.1: the game tests of {@link McpjGameTests} as test functions; each loader registers this class. */
public final class McpjGameTestGenerator {
    private static final String BATCH = "mcpj";
    private static final String STRUCTURE = "mcpj:platform";
    private static final long SETUP_TICKS = 0L;

    /** Public: Fabric instantiates its game test entrypoints (the method is static all the same). */
    public McpjGameTestGenerator() {}

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        return McpjGameTests.ALL.stream()
                .map(test -> new TestFunction(BATCH, BATCH + "." + test.name(), STRUCTURE, test.maxTicks(), SETUP_TICKS, true, test.function()))
                .toList();
    }
}
