package de.mcpj.core.config;

/** A int setting; returns the current value (it may change when the config is reloaded or synced). */
@FunctionalInterface
public interface IntSetting {
    int get();
}
