package de.mcpj.forge.registry;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.registry.McpjObjects;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, McpjMod.MOD_ID);

    public static final RegistryObject<Block> CHARGING_PAD = BLOCKS.register(McpjObjects.CHARGING_PAD, McpjObjects::chargingPad);
    public static final RegistryObject<Block> JUMPER_PROJECTOR = BLOCKS.register(McpjObjects.JUMPER_PROJECTOR, McpjObjects::jumperProjector);

    private ModBlocks() {}
}
