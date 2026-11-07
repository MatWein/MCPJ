package de.mcpj.mc.block;

import de.mcpj.core.util.SaveData;
import de.mcpj.mc.util.NbtSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minecraft 1.20.1's side of MCPJ's block entities: saving, which differs between Minecraft versions. The block
 * entities themselves are version-independent and store their data through {@link SaveData}.
 */
public abstract class SavedBlockEntity extends BlockEntity {
    protected SavedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract void loadData(SaveData data);

    protected abstract void saveData(SaveData data);

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadData(NbtSaveData.reading(tag));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveData(NbtSaveData.writing(tag));
    }
}
