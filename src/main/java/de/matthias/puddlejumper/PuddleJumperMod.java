package de.matthias.puddlejumper;

import de.matthias.puddlejumper.config.ClientConfig;
import de.matthias.puddlejumper.config.ServerConfig;
import de.matthias.puddlejumper.network.ModNetwork;
import de.matthias.puddlejumper.registry.ModCreativeTabs;
import de.matthias.puddlejumper.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PuddleJumperMod.MOD_ID)
public final class PuddleJumperMod {
    public static final String MOD_ID = "puddlejumper";

    public PuddleJumperMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModRegistries.register(modBus);
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modBus.addListener(PuddleJumperMod::onCommonSetup);
        modBus.addListener(ModCreativeTabs::onBuildContents);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
