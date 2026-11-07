package de.mcpj.mc.util;

import de.mcpj.core.util.SaveData;
import de.mcpj.core.math.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.List;

/** The core's save data stored in the entity's NBT (positions as packed longs, as before). */
public record NbtSaveData(CompoundTag tag) implements SaveData {
    public static NbtSaveData reading(CompoundTag tag) {
        return new NbtSaveData(tag);
    }

    public static NbtSaveData writing(CompoundTag tag) {
        return new NbtSaveData(tag);
    }

    @Override
    public boolean contains(String key) {
        return tag.contains(key);
    }

    @Override
    public int getInt(String key) {
        return tag.getInt(key);
    }

    @Override
    public void putInt(String key, int value) {
        tag.putInt(key, value);
    }

    @Override
    public float getFloat(String key) {
        return tag.getFloat(key);
    }

    @Override
    public void putFloat(String key, float value) {
        tag.putFloat(key, value);
    }

    @Override
    public boolean getBoolean(String key) {
        return tag.getBoolean(key);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        tag.putBoolean(key, value);
    }

    @Override
    public List<BlockPosition> getPositions(String key) {
        return Arrays.stream(tag.getLongArray(key)).mapToObj(packed -> Convert.toCore(BlockPos.of(packed))).toList();
    }

    @Override
    public void putPositions(String key, List<BlockPosition> positions) {
        tag.putLongArray(key, positions.stream().mapToLong(pos -> Convert.toMc(pos).asLong()).toArray());
    }
}
