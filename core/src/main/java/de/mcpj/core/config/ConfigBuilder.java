package de.mcpj.core.config;

/**
 * Declares settings (name, default, range, comment) independent of a mod loader's config system.
 *
 * <p>Each platform module implements it on top of its config backend (e.g. Forge's ForgeConfigSpec). The shape
 * follows the common builder style: {@code builder.comment("...").defineInRange("name", 1.0, 0.0, 2.0)}.
 */
public interface ConfigBuilder {
    /** Comment for the next section or setting. */
    ConfigBuilder comment(String... lines);

    /** Opens a section (nested name); close it with {@link #pop()}. */
    ConfigBuilder push(String section);

    ConfigBuilder pop();

    DoubleSetting defineInRange(String name, double defaultValue, double min, double max);

    IntSetting defineInRange(String name, int defaultValue, int min, int max);

    BooleanSetting define(String name, boolean defaultValue);
}
