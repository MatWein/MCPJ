package de.mcpj;

import de.mcpj.compat.StargateCompat;
import de.mcpj.config.ClientConfig;
import de.mcpj.config.ServerConfig;
import de.mcpj.network.ModNetwork;
import de.mcpj.registry.ModCreativeTabs;
import de.mcpj.registry.ModRegistries;
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
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
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
