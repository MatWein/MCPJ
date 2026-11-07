package de.mcpj.neoforge.registry;

import de.mcpj.mc.block.JumperProjectorBlockEntity;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.registry.McpjObjects;
import de.mcpj.neoforge.block.NeoForgeChargingPadBlockEntity;
import de.mcpj.neoforge.entity.NeoForgePuddleJumperEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


/** MCPJ's objects (built by McpjObjects), registered through NeoForge's deferred registers. */
public final class NeoForgeObjects {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Mcpj.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Mcpj.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Mcpj.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mcpj.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Mcpj.MOD_ID);

    public static final DeferredHolder<Block, Block> CHARGING_PAD = BLOCKS.register(McpjObjects.CHARGING_PAD, McpjObjects::chargingPad);
    public static final DeferredHolder<Block, Block> JUMPER_PROJECTOR = BLOCKS.register(McpjObjects.JUMPER_PROJECTOR, McpjObjects::jumperProjector);
    public static final DeferredHolder<Item, Item> PUDDLE_JUMPER_ITEM = ITEMS.register(McpjObjects.PUDDLE_JUMPER, McpjObjects::jumperItem);
    public static final DeferredHolder<Item, Item> CHARGING_PAD_ITEM =
            ITEMS.register(McpjObjects.CHARGING_PAD, () -> McpjObjects.chargingPadItem(CHARGING_PAD.get()));
    public static final DeferredHolder<Item, Item> JUMPER_PROJECTOR_ITEM =
            ITEMS.register(McpjObjects.JUMPER_PROJECTOR, () -> McpjObjects.jumperProjectorItem(JUMPER_PROJECTOR.get()));
    public static final DeferredHolder<EntityType<?>, EntityType<NeoForgePuddleJumperEntity>> PUDDLE_JUMPER =
            ENTITY_TYPES.register(McpjObjects.PUDDLE_JUMPER, () -> McpjObjects.jumperType(NeoForgePuddleJumperEntity::new));
    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE =
            ENTITY_TYPES.register(McpjObjects.DRONE, McpjObjects::droneType);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NeoForgeChargingPadBlockEntity>> CHARGING_PAD_ENTITY =
            BLOCK_ENTITY_TYPES.register(McpjObjects.CHARGING_PAD,
                    () -> McpjObjects.chargingPadEntity(NeoForgeChargingPadBlockEntity::new, CHARGING_PAD.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JumperProjectorBlockEntity>> JUMPER_PROJECTOR_ENTITY =
            BLOCK_ENTITY_TYPES.register(McpjObjects.JUMPER_PROJECTOR, () -> McpjObjects.jumperProjectorEntity(JUMPER_PROJECTOR.get()));
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE = SOUND_EVENTS.register(McpjObjects.ENGINE, McpjObjects::engineSound);

    private NeoForgeObjects() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        SOUND_EVENTS.register(modBus);
        modBus.addListener(NeoForgeObjects::onBuildContents);
    }

    private static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PUDDLE_JUMPER_ITEM.get());
            event.accept(CHARGING_PAD_ITEM.get());
            event.accept(JUMPER_PROJECTOR_ITEM.get());
        }
    }
}
