package de.matthias.puddlejumper;

import de.matthias.puddlejumper.entity.DroneEntity;
import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import de.matthias.puddlejumper.item.PuddleJumperItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;

public final class ModRegistry {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<EntityType<PuddleJumperEntity>> PUDDLE_JUMPER = ENTITIES.register("puddle_jumper",
            () -> EntityType.Builder.<PuddleJumperEntity>of(PuddleJumperEntity::new, MobCategory.MISC)
                    .sized(PuddleJumperEntity.WIDTH, PuddleJumperEntity.HEIGHT)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "puddle_jumper").toString()));

    public static final RegistryObject<EntityType<DroneEntity>> DRONE = ENTITIES.register("drone",
            () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "drone").toString()));

    public static final RegistryObject<Item> PUDDLE_JUMPER_ITEM = ITEMS.register("puddle_jumper",
            () -> new PuddleJumperItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<SoundEvent> ENGINE = SOUNDS.register("engine",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "engine")));

    private ModRegistry() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
    }
}
