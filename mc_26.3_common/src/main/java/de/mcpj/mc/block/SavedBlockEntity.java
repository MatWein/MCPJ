package de.mcpj.mc.block;

import de.mcpj.core.util.SaveData;
import de.mcpj.mc.util.NbtSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minecraft 26.3's side of MCPJ's block entities: saving, which differs between Minecraft versions. The block
 * entities themselves are version-independent and store their data through {@link SaveData}.
 */
public abstract class SavedBlockEntity extends BlockEntity {
    protected SavedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract void loadData(SaveData data);

    protected abstract void saveData(SaveData data);

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadData(NbtSaveData.reading(input));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        saveData(NbtSaveData.writing(output));
    }
}
