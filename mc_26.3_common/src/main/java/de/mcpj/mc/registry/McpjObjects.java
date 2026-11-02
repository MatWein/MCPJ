package de.mcpj.mc.registry;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.block.ChargingPadBlock;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.item.PuddleJumperItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.Set;

/**
 * Names and construction of MCPJ's blocks, items, entity types, block entity types and sounds. The loaders only
 * register them (each in its own way) and pass their own subclasses where they add loader features.
 */
public final class McpjObjects {
    public static final String PUDDLE_JUMPER = "puddle_jumper";
    public static final String DRONE = "drone";
    public static final String CHARGING_PAD = "charging_pad";
    public static final String ENGINE = "engine";

    private static final float PAD_HARDNESS = 3.5f;
    private static final float PAD_BLAST_RESISTANCE = 6.0f;
    private static final int JUMPER_TRACKING_RANGE = 10;
    private static final int JUMPER_UPDATE_INTERVAL = 2;
    private static final int DRONE_TRACKING_RANGE = 8;
    private static final int DRONE_UPDATE_INTERVAL = 1;

    private McpjObjects() {}

    public static Block chargingPad() {
        return new ChargingPadBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Mcpj.id(CHARGING_PAD)))
                .mapColor(MapColor.METAL)
                .strength(PAD_HARDNESS, PAD_BLAST_RESISTANCE)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .lightLevel(ChargingPadBlock::lightLevel));
    }

    public static Item jumperItem() {
        return new PuddleJumperItem(new Item.Properties().setId(itemKey(PUDDLE_JUMPER)).stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    public static Item chargingPadItem(Block pad) {
        return new BlockItem(pad, new Item.Properties().setId(itemKey(CHARGING_PAD)).useBlockDescriptionPrefix().rarity(Rarity.RARE));
    }

    /** The jumper's entity type; the loader passes the constructor of its jumper subclass. */
    public static <T extends PuddleJumperEntity> EntityType<T> jumperType(EntityType.EntityFactory<T> factory) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(JumperGeometry.WIDTH, JumperGeometry.HEIGHT)
                .eyeHeight(JumperGeometry.HEIGHT * PuddleJumperEntity.EYE_HEIGHT_FACTOR)
                .fireImmune()
                .clientTrackingRange(JUMPER_TRACKING_RANGE)
                .updateInterval(JUMPER_UPDATE_INTERVAL)
                .build(entityKey(PUDDLE_JUMPER));
    }

    public static EntityType<DroneEntity> droneType() {
        return EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                .sized(DroneEntity.SIZE, DroneEntity.SIZE)
                .fireImmune()
                .clientTrackingRange(DRONE_TRACKING_RANGE)
                .updateInterval(DRONE_UPDATE_INTERVAL)
                .build(entityKey(DRONE));
    }

    /** The charging pad's block entity type; the loader passes the constructor of its subclass (energy API). */
    public static <T extends ChargingPadBlockEntity> BlockEntityType<T> chargingPadEntity(BlockEntityType.BlockEntitySupplier<T> factory, Block pad) {
        return new BlockEntityType<>(factory, Set.of(pad));
    }

    public static SoundEvent engineSound() {
        return SoundEvent.createVariableRangeEvent(Mcpj.id(ENGINE));
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Mcpj.id(name));
    }

    private static ResourceKey<EntityType<?>> entityKey(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Mcpj.id(name));
    }
}
