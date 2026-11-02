package de.mcpj.forge.config;

import de.mcpj.core.config.BooleanSetting;
import de.mcpj.core.config.ConfigBuilder;
import de.mcpj.core.config.DoubleSetting;
import de.mcpj.core.config.IntSetting;
import net.minecraftforge.common.ForgeConfigSpec;

/** The core's settings on top of Forge's config system. */
final class ForgeConfigBuilder implements ConfigBuilder {
    private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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
        ForgeConfigSpec.DoubleValue value = builder.defineInRange(name, defaultValue, min, max);
        return value::get;
    }

    @Override
    public IntSetting defineInRange(String name, int defaultValue, int min, int max) {
        ForgeConfigSpec.IntValue value = builder.defineInRange(name, defaultValue, min, max);
        return value::get;
    }

    @Override
    public BooleanSetting define(String name, boolean defaultValue) {
        ForgeConfigSpec.BooleanValue value = builder.define(name, defaultValue);
        return value::get;
    }

    ForgeConfigSpec build() {
        return builder.build();
    }
}
