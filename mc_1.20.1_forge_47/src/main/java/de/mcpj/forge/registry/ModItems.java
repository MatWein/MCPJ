package de.mcpj.forge.registry;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.item.PuddleJumperItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, McpjMod.MOD_ID);

    public static final RegistryObject<Item> PUDDLE_JUMPER = ITEMS.register("puddle_jumper",
            () -> new PuddleJumperItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> CHARGING_PAD = ITEMS.register("charging_pad",
            () -> new BlockItem(ModBlocks.CHARGING_PAD.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> JUMPER_PROJECTOR = ITEMS.register("jumper_projector",
            () -> new BlockItem(ModBlocks.JUMPER_PROJECTOR.get(), new Item.Properties().rarity(Rarity.EPIC)));

    private ModItems() {}
}
