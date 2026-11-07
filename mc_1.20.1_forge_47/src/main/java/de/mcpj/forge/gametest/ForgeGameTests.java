package de.mcpj.forge.gametest;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.gametest.McpjGameTestGenerator;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.Collection;

/** Forge only runs game tests of classes marked as its mod's (forge.enabledGameTestNamespaces): MCPJ's shared tests. */
@GameTestHolder(McpjMod.MOD_ID)
public final class ForgeGameTests {
    private ForgeGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        return McpjGameTestGenerator.generate();
    }
}
