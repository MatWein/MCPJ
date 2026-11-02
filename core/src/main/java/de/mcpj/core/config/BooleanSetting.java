package de.mcpj.core.config;

/** A boolean setting; returns the current value (it may change when the config is reloaded or synced). */
@FunctionalInterface
public interface BooleanSetting {
    boolean get();
}
