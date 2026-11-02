package de.mcpj.forge;

import de.mcpj.forge.gametest.ForgeGameTestHooks;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.gametest.GameTestHooks;
import de.mcpj.mc.platform.McpjPlatform;
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
/** MCPJ for Minecraft 1.20.1 on Forge 47. */
public final class McpjMod {
    public static final String MOD_ID = Mcpj.MOD_ID;

    public McpjMod(FMLJavaModLoadingContext context) {
        McpjPlatform.set(new ForgePlatform());
        GameTestHooks.set(new ForgeGameTestHooks());
        IEventBus modBus = context.getModEventBus();
        ModRegistries.register(modBus);
        context.registerConfig(ModConfig.Type.SERVER, ForgeConfigs.SERVER);
        context.registerConfig(ModConfig.Type.CLIENT, ForgeConfigs.CLIENT);
        modBus.addListener(McpjMod::onCommonSetup);
        modBus.addListener(ModCreativeTabs::onBuildContents);
        StargateCompat.register();
    }

    public static ResourceLocation id(String path) {
        return Mcpj.id(path);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
