package de.mcpj.core.config;

/** A double setting; returns the current value (it may change when the config is reloaded or synced). */
@FunctionalInterface
public interface DoubleSetting {
    double get();
}
