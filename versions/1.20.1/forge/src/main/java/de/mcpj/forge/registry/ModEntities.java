package de.mcpj.forge.registry;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.registry.McpjObjects;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.forge.entity.jumper.ForgePuddleJumperEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, McpjMod.MOD_ID);

    public static final RegistryObject<EntityType<ForgePuddleJumperEntity>> PUDDLE_JUMPER = ENTITIES.register(McpjObjects.PUDDLE_JUMPER,
            () -> McpjObjects.jumperType(ForgePuddleJumperEntity::new));
    public static final RegistryObject<EntityType<DroneEntity>> DRONE = ENTITIES.register(McpjObjects.DRONE, McpjObjects::droneType);

    private ModEntities() {}
}
