package de.mcpj.core.config;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Test helper: every setting returns its default; records problems with ranges and sections. */
public final class DefaultConfigBuilder implements ConfigBuilder {
    private final Deque<String> sections = new ArrayDeque<>();
    final List<String> problems = new ArrayList<>();
    int settings;

    /** Defines the server and client config with their defaults (call before using ServerConfig in tests). */
    public static void useDefaults() {
        useDefaultsExcept(Map.of());
    }

    /** Like {@link #useDefaults()}, but the named settings (by name, any section) take the given values. */
    public static void useDefaultsExcept(Map<String, Object> values) {
        DefaultConfigBuilder server = new DefaultConfigBuilder();
        server.overrides.putAll(values);
        ServerConfig.define(server);
        ClientConfig.define(new DefaultConfigBuilder());
    }

    private final Map<String, Object> overrides = new HashMap<>();

    @Override
    public ConfigBuilder comment(String... lines) {
        return this;
    }

    @Override
    public ConfigBuilder push(String section) {
        sections.push(section);
        return this;
    }

    @Override
    public ConfigBuilder pop() {
        if (sections.isEmpty()) problems.add("pop without push");
        else sections.pop();
        return this;
    }

    @Override
    public DoubleSetting defineInRange(String name, double defaultValue, double min, double max) {
        check(name, defaultValue >= min && defaultValue <= max && min <= max);
        double value = (Double) overrides.getOrDefault(name, defaultValue);
        return () -> value;
    }

    @Override
    public IntSetting defineInRange(String name, int defaultValue, int min, int max) {
        check(name, defaultValue >= min && defaultValue <= max && min <= max);
        int value = (Integer) overrides.getOrDefault(name, defaultValue);
        return () -> value;
    }

    @Override
    public BooleanSetting define(String name, boolean defaultValue) {
        check(name, true);
        boolean value = (Boolean) overrides.getOrDefault(name, defaultValue);
        return () -> value;
    }

    boolean sectionsClosed() {
        return sections.isEmpty();
    }

    private void check(String name, boolean inRange) {
        settings++;
        if (sections.isEmpty()) problems.add(name + " is outside of any section");
        if (!inRange) problems.add(name + " default is outside its range");
    }
}
