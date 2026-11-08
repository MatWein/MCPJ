package de.mcpj.forge.registry;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.registry.McpjObjects;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, McpjMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MCPJ = TABS.register(McpjObjects.CREATIVE_TAB,
            () -> McpjObjects.creativeTab(CreativeModeTab.builder(), ModItems.PUDDLE_JUMPER).displayItems((parameters, output) -> {
                output.accept(ModItems.PUDDLE_JUMPER.get());
                output.accept(ModItems.JUMPER_KEY.get());
                output.accept(ModItems.CHARGING_PAD.get());
                output.accept(ModItems.JUMPER_PROJECTOR.get());
            }).build());

    private ModCreativeTabs() {}
}
