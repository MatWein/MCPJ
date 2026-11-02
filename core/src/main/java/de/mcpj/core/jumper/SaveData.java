package de.mcpj.core.jumper;

import de.mcpj.core.math.BlockPosition;

import java.util.List;

/** Persistent key-value data of a ship (the platform stores it, e.g. as NBT). */
public interface SaveData {
    boolean contains(String key);

    int getInt(String key);

    void putInt(String key, int value);

    float getFloat(String key);

    void putFloat(String key, float value);

    boolean getBoolean(String key);

    void putBoolean(String key, boolean value);

    List<BlockPosition> getPositions(String key);

    void putPositions(String key, List<BlockPosition> positions);
}
