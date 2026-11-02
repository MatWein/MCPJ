package de.matthias.puddlejumper.registry;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.entity.drone.DroneEntity;
import de.matthias.puddlejumper.entity.jumper.JumperGeometry;
import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PuddleJumperMod.MOD_ID);

    private static final int JUMPER_TRACKING_RANGE = 10;
    private static final int JUMPER_UPDATE_INTERVAL = 2;
    private static final int DRONE_TRACKING_RANGE = 8;
    private static final int DRONE_UPDATE_INTERVAL = 1;

    public static final RegistryObject<EntityType<PuddleJumperEntity>> PUDDLE_JUMPER = ENTITIES.register("puddle_jumper",
            () -> EntityType.Builder.<PuddleJumperEntity>of(PuddleJumperEntity::new, MobCategory.MISC)
                    .sized(JumperGeometry.WIDTH, JumperGeometry.HEIGHT)
                    .fireImmune()
                    .clientTrackingRange(JUMPER_TRACKING_RANGE)
                    .updateInterval(JUMPER_UPDATE_INTERVAL)
                    .build(PuddleJumperMod.id("puddle_jumper").toString()));

    public static final RegistryObject<EntityType<DroneEntity>> DRONE = ENTITIES.register("drone",
            () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                    .sized(DroneEntity.SIZE, DroneEntity.SIZE)
                    .fireImmune()
                    .clientTrackingRange(DRONE_TRACKING_RANGE)
                    .updateInterval(DRONE_UPDATE_INTERVAL)
                    .build(PuddleJumperMod.id("drone").toString()));

    private ModEntities() {}
}
