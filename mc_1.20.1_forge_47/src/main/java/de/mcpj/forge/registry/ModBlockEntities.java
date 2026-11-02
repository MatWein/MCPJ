package de.mcpj.forge.registry;

import de.mcpj.forge.McpjMod;
import de.mcpj.forge.block.ChargingPadBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, McpjMod.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ChargingPadBlockEntity>> CHARGING_PAD = BLOCK_ENTITIES.register("charging_pad",
            () -> BlockEntityType.Builder.of(ChargingPadBlockEntity::new, ModBlocks.CHARGING_PAD.get()).build(null));

    private ModBlockEntities() {}
}
