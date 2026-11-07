package de.mcpj.mc.util;

import net.minecraft.network.syncher.EntityDataAccessor;

/**
 * Defines an entity's synced data with its initial value. Minecraft hands entities either its synced data itself
 * (1.20.1) or a builder for it (1.20.5 and later); each version's compat class passes the matching one.
 */
public interface SyncedDataDefiner {
    <T> void define(EntityDataAccessor<T> key, T value);
}
