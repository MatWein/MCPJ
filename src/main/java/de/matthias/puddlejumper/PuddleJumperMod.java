package de.matthias.puddlejumper;

import de.matthias.puddlejumper.network.ModNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PuddleJumperMod.MODID)
public class PuddleJumperMod {
    public static final String MODID = "puddlejumper";

    public PuddleJumperMod(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ModRegistry.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModRegistry.PUDDLE_JUMPER_ITEM);
        }
    }
}
