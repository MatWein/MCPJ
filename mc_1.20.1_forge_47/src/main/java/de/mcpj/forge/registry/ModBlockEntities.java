package de.mcpj.forge.registry;

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

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ForgeChargingPadBlockEntity>> CHARGING_PAD = BLOCK_ENTITIES.register("charging_pad",
            () -> BlockEntityType.Builder.of(ForgeChargingPadBlockEntity::new, ModBlocks.CHARGING_PAD.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<JumperProjectorBlockEntity>> JUMPER_PROJECTOR = BLOCK_ENTITIES.register("jumper_projector",
            () -> BlockEntityType.Builder.of(JumperProjectorBlockEntity::new, ModBlocks.JUMPER_PROJECTOR.get()).build(null));

    private ModBlockEntities() {}
}
