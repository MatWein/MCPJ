package de.mcpj.mc.block;

import de.mcpj.core.util.SaveData;
import de.mcpj.mc.util.NbtSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minecraft 1.21.1's side of MCPJ's block entities: saving, which differs between Minecraft versions. The block
 * entities themselves are version-independent and store their data through {@link SaveData}.
 */
public abstract class SavedBlockEntity extends BlockEntity {
    protected SavedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract void loadData(SaveData data);

    protected abstract void saveData(SaveData data);

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadData(NbtSaveData.reading(tag));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveData(NbtSaveData.writing(tag));
    }
}
