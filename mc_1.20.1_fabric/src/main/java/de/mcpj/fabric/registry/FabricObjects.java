package de.mcpj.fabric.registry;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.fabric.block.FabricChargingPadBlockEntity;
import de.mcpj.fabric.entity.FabricPuddleJumperEntity;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.block.ChargingPadBlock;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.item.PuddleJumperItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Blocks, items, entities, block entities and sounds of MCPJ, registered with vanilla's registries. */
public final class FabricObjects {
    private static final float PAD_HARDNESS = 3.5f;
    private static final float PAD_BLAST_RESISTANCE = 6.0f;
    private static final int JUMPER_TRACKING_RANGE = 10;
    private static final int JUMPER_UPDATE_INTERVAL = 2;
    private static final int DRONE_TRACKING_RANGE = 8;
    private static final int DRONE_UPDATE_INTERVAL = 1;

    public static final Block CHARGING_PAD = new ChargingPadBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(PAD_HARDNESS, PAD_BLAST_RESISTANCE)
            .requiresCorrectToolForDrops()
            .sound(SoundType.METAL)
            .lightLevel(ChargingPadBlock::lightLevel));
    public static final Item PUDDLE_JUMPER_ITEM = new PuddleJumperItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final Item CHARGING_PAD_ITEM = new BlockItem(CHARGING_PAD, new Item.Properties().rarity(Rarity.RARE));
    public static final EntityType<FabricPuddleJumperEntity> PUDDLE_JUMPER = EntityType.Builder
            .<FabricPuddleJumperEntity>of(FabricPuddleJumperEntity::new, MobCategory.MISC)
            .sized(JumperGeometry.WIDTH, JumperGeometry.HEIGHT)
            .fireImmune()
            .clientTrackingRange(JUMPER_TRACKING_RANGE)
            .updateInterval(JUMPER_UPDATE_INTERVAL)
            .build(Mcpj.id("puddle_jumper").toString());
    public static final EntityType<DroneEntity> DRONE = EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
            .sized(DroneEntity.SIZE, DroneEntity.SIZE)
            .fireImmune()
            .clientTrackingRange(DRONE_TRACKING_RANGE)
            .updateInterval(DRONE_UPDATE_INTERVAL)
            .build(Mcpj.id("drone").toString());
    public static final BlockEntityType<FabricChargingPadBlockEntity> CHARGING_PAD_ENTITY =
            BlockEntityType.Builder.of(FabricChargingPadBlockEntity::new, CHARGING_PAD).build(null);
    public static final SoundEvent ENGINE = SoundEvent.createVariableRangeEvent(Mcpj.id("engine"));

    private FabricObjects() {}

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, Mcpj.id("charging_pad"), CHARGING_PAD);
        Registry.register(BuiltInRegistries.ITEM, Mcpj.id("puddle_jumper"), PUDDLE_JUMPER_ITEM);
        Registry.register(BuiltInRegistries.ITEM, Mcpj.id("charging_pad"), CHARGING_PAD_ITEM);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Mcpj.id("puddle_jumper"), PUDDLE_JUMPER);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, Mcpj.id("drone"), DRONE);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Mcpj.id("charging_pad"), CHARGING_PAD_ENTITY);
        Registry.register(BuiltInRegistries.SOUND_EVENT, Mcpj.id("engine"), ENGINE);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(PUDDLE_JUMPER_ITEM);
            entries.accept(CHARGING_PAD_ITEM);
        });
    }
}
