package de.mcpj.forge.registry;

import de.mcpj.mc.registry.McpjObjects;
import de.mcpj.forge.McpjMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, McpjMod.MOD_ID);

    public static final RegistryObject<Item> PUDDLE_JUMPER = ITEMS.register(McpjObjects.PUDDLE_JUMPER, McpjObjects::jumperItem);
    public static final RegistryObject<Item> CHARGING_PAD = ITEMS.register(McpjObjects.CHARGING_PAD,
            () -> McpjObjects.chargingPadItem(ModBlocks.CHARGING_PAD.get()));
    public static final RegistryObject<Item> JUMPER_PROJECTOR = ITEMS.register(McpjObjects.JUMPER_PROJECTOR,
            () -> McpjObjects.jumperProjectorItem(ModBlocks.JUMPER_PROJECTOR.get()));

    private ModItems() {}
}
