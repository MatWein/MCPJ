package de.mcpj.mc.util;

import com.mojang.serialization.Codec;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.util.SaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The core's save data in Minecraft's save format (positions as packed longs, as in 1.20.1). Minecraft separates
 * reading ({@link ValueInput}) from writing ({@link ValueOutput}), so an instance does one of the two.
 */
public final class NbtSaveData implements SaveData {
    private static final Codec<List<Long>> PACKED_POSITIONS = Codec.LONG_STREAM.xmap(
            stream -> stream.boxed().toList(), list -> list.stream().mapToLong(Long::longValue));

    @Nullable private final ValueInput in;
    @Nullable private final ValueOutput out;

    private NbtSaveData(@Nullable ValueInput in, @Nullable ValueOutput out) {
        this.in = in;
        this.out = out;
    }

    public static NbtSaveData reading(ValueInput in) {
        return new NbtSaveData(in, null);
    }

    public static NbtSaveData writing(ValueOutput out) {
        return new NbtSaveData(null, out);
    }

    private ValueInput in() {
        if (in == null) throw new IllegalStateException("save data is write-only");
        return in;
    }

    private ValueOutput out() {
        if (out == null) throw new IllegalStateException("save data is read-only");
        return out;
    }

    @Override
    public boolean contains(String key) {
        return in().read(key, Codec.PASSTHROUGH).isPresent();
    }

    @Override
    public int getInt(String key) {
        return in().getIntOr(key, 0);
    }

    @Override
    public void putInt(String key, int value) {
        out().putInt(key, value);
    }

    @Override
    public float getFloat(String key) {
        return in().getFloatOr(key, 0);
    }

    @Override
    public void putFloat(String key, float value) {
        out().putFloat(key, value);
    }

    @Override
    public boolean getBoolean(String key) {
        return in().getBooleanOr(key, false);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        out().putBoolean(key, value);
    }

    @Override
    public List<BlockPosition> getPositions(String key) {
        return in().read(key, PACKED_POSITIONS).orElse(List.of()).stream()
                .map(packed -> Convert.toCore(BlockPos.of(packed))).toList();
    }

    @Override
    public void putPositions(String key, List<BlockPosition> positions) {
        out().store(key, PACKED_POSITIONS, positions.stream().map(pos -> Convert.toMc(pos).asLong()).toList());
    }
}
