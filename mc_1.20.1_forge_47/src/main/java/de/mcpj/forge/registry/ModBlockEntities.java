package de.mcpj.forge.registry;

import de.mcpj.mc.registry.McpjObjects;
import de.mcpj.mc.block.JumperProjectorBlockEntity;
import de.mcpj.forge.McpjMod;
import de.mcpj.forge.block.ForgeChargingPadBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, McpjMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ForgeChargingPadBlockEntity>> CHARGING_PAD = BLOCK_ENTITIES.register(McpjObjects.CHARGING_PAD,
            () -> McpjObjects.chargingPadEntity(ForgeChargingPadBlockEntity::new, ModBlocks.CHARGING_PAD.get()));
    public static final RegistryObject<BlockEntityType<JumperProjectorBlockEntity>> JUMPER_PROJECTOR = BLOCK_ENTITIES.register(McpjObjects.JUMPER_PROJECTOR,
            () -> McpjObjects.jumperProjectorEntity(ModBlocks.JUMPER_PROJECTOR.get()));

    private ModBlockEntities() {}
}
