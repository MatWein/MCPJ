package de.mcpj.core.util;

import de.mcpj.core.math.BlockPosition;

import java.util.List;
import java.util.UUID;

/** Persistent key-value data of a ship or block (the platform stores it, e.g. as NBT). */
public interface SaveData {
    boolean contains(String key);

    int getInt(String key);

    void putInt(String key, int value);

    float getFloat(String key);

    void putFloat(String key, float value);

    /** The text, or an empty one if it is missing. */
    String getString(String key);

    void putString(String key, String value);

    boolean getBoolean(String key);

    void putBoolean(String key, boolean value);

    List<BlockPosition> getPositions(String key);

    void putPositions(String key, List<BlockPosition> positions);

    /** The stored id, or null if there is none. */
    UUID getUuid(String key);

    /** Stores the id; null stores nothing. */
    void putUuid(String key, UUID id);
}
