package de.mcpj.forge;

import de.mcpj.forge.compat.StargateCompat;
import de.mcpj.forge.config.ForgeConfigs;
import de.mcpj.forge.network.ModNetwork;
import de.mcpj.forge.registry.ModCreativeTabs;
import de.mcpj.forge.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(McpjMod.MOD_ID)
public final class McpjMod {
    public static final String MOD_ID = "mcpj";

    public McpjMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModRegistries.register(modBus);
        context.registerConfig(ModConfig.Type.SERVER, ForgeConfigs.SERVER);
        context.registerConfig(ModConfig.Type.CLIENT, ForgeConfigs.CLIENT);
        modBus.addListener(McpjMod::onCommonSetup);
        modBus.addListener(ModCreativeTabs::onBuildContents);
        StargateCompat.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
