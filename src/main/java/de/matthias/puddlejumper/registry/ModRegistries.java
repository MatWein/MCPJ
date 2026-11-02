package de.matthias.puddlejumper.registry;

import net.minecraftforge.eventbus.api.IEventBus;

public final class ModRegistries {
    private ModRegistries() {}

    public static void register(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
    }
}
