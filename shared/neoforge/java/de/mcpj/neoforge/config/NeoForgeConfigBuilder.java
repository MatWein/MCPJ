package de.mcpj.neoforge.config;

import de.mcpj.core.config.BooleanSetting;
import de.mcpj.core.config.ConfigBuilder;
import de.mcpj.core.config.DoubleSetting;
import de.mcpj.core.config.IntSetting;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The core's settings on top of NeoForge's config system. */
final class NeoForgeConfigBuilder implements ConfigBuilder {
    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    @Override
    public ConfigBuilder comment(String... lines) {
        builder.comment(lines);
        return this;
    }

    @Override
    public ConfigBuilder push(String section) {
        builder.push(section);
        return this;
    }

    @Override
    public ConfigBuilder pop() {
        builder.pop();
        return this;
    }

    @Override
    public DoubleSetting defineInRange(String name, double defaultValue, double min, double max) {
        ModConfigSpec.DoubleValue value = builder.defineInRange(name, defaultValue, min, max);
        return value::get;
    }

    @Override
    public IntSetting defineInRange(String name, int defaultValue, int min, int max) {
        ModConfigSpec.IntValue value = builder.defineInRange(name, defaultValue, min, max);
        return value::get;
    }

    @Override
    public BooleanSetting define(String name, boolean defaultValue) {
        ModConfigSpec.BooleanValue value = builder.define(name, defaultValue);
        return value::get;
    }

    ModConfigSpec build() {
        return builder.build();
    }
}
