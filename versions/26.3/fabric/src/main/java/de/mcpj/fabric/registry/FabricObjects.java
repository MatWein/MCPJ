package de.mcpj.fabric.registry;

import de.mcpj.mc.block.JumperProjectorBlockEntity;
import de.mcpj.fabric.block.FabricChargingPadBlockEntity;
import de.mcpj.fabric.entity.FabricPuddleJumperEntity;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.gametest.McpjGameTests;
import de.mcpj.mc.registry.McpjObjects;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** MCPJ's objects (built by McpjObjects), registered with vanilla's registries. */
public final class FabricObjects {
    public static final Block CHARGING_PAD = McpjObjects.chargingPad();
    public static final Block JUMPER_PROJECTOR = McpjObjects.jumperProjector();
    public static final Item PUDDLE_JUMPER_ITEM = McpjObjects.jumperItem();
    public static final Item CHARGING_PAD_ITEM = McpjObjects.chargingPadItem(CHARGING_PAD);
    public static final Item JUMPER_PROJECTOR_ITEM = McpjObjects.jumperProjectorItem(JUMPER_PROJECTOR);
    public static final EntityType<FabricPuddleJumperEntity> PUDDLE_JUMPER = McpjObjects.jumperType(FabricPuddleJumperEntity::new);
    public static final EntityType<DroneEntity> DRONE = McpjObjects.droneType();
    public static final BlockEntityType<FabricChargingPadBlockEntity> CHARGING_PAD_ENTITY =
            McpjObjects.chargingPadEntity(FabricChargingPadBlockEntity::new, CHARGING_PAD);
    public static final BlockEntityType<JumperProjectorBlockEntity> JUMPER_PROJECTOR_ENTITY = McpjObjects.jumperProjectorEntity(JUMPER_PROJECTOR);
    public static final SoundEvent ENGINE = McpjObjects.engineSound();

    private FabricObjects() {}

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, Mcpj.id(McpjObjects.CHARGING_PAD), CHARGING_PAD);
        Registry.register(BuiltInRegistries.BLOCK, Mcpj.id(McpjObjects.JUMPER_PROJECTOR), JUMPER_PROJECTOR);
        Registry.register(BuiltInRegistries.ITEM, Mcpj.id(McpjObjects.PUDDLE_JUMPER), PUDDLE_JUMPER_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Mcpj.id(McpjObjects.CHARGING_PAD), CHARGING_PAD_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Mcpj.id(McpjObjects.JUMPER_PROJECTOR), JUMPER_PROJECTOR_ITEM);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Mcpj.id(McpjObjects.PUDDLE_JUMPER), PUDDLE_JUMPER);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Mcpj.id(McpjObjects.DRONE), DRONE);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Mcpj.id(McpjObjects.CHARGING_PAD), CHARGING_PAD_ENTITY);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Mcpj.id(McpjObjects.JUMPER_PROJECTOR), JUMPER_PROJECTOR_ENTITY);
        Registry.register(BuiltInRegistries.SOUND_EVENT, Mcpj.id(McpjObjects.ENGINE), ENGINE);
        McpjGameTests.ALL.forEach(test -> Registry.register(BuiltInRegistries.TEST_FUNCTION, Mcpj.id(test.name()), test.function()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            output.accept(PUDDLE_JUMPER_ITEM);
            output.accept(CHARGING_PAD_ITEM);
            output.accept(JUMPER_PROJECTOR_ITEM);
        });
    }
}
