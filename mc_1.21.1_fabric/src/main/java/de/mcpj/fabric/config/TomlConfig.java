package de.mcpj.fabric.config;

import com.mojang.logging.LogUtils;
import de.mcpj.core.config.BooleanSetting;
import de.mcpj.core.config.ConfigBuilder;
import de.mcpj.core.config.DoubleSetting;
import de.mcpj.core.config.IntSetting;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The core's settings in a small TOML file (same sections, keys and comments as Forge's config). Values outside their
 * range fall back to the default; missing entries are written back with their defaults.
 */
public final class TomlConfig implements ConfigBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();

    private enum Kind { DOUBLE, INT, BOOLEAN }

    private static final class Entry {
        final String section;
        final String name;
        final List<String> comment;
        final Kind kind;
        final double defaultValue;
        final double min;
        final double max;
        volatile double value;

        Entry(String section, String name, List<String> comment, Kind kind, double defaultValue, double min, double max) {
            this.section = section;
            this.name = name;
            this.comment = comment;
            this.kind = kind;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.value = defaultValue;
        }

        String path() {
            return section.isEmpty() ? name : section + "." + name;
        }

        String format() {
            return switch (kind) {
                case DOUBLE -> Double.toString(value);
                case INT -> Long.toString((long) value);
                case BOOLEAN -> Boolean.toString(value != 0);
            };
        }

        /** Sets the value from text; false if it is invalid or out of range. */
        boolean parse(String text) {
            try {
                double parsed = switch (kind) {
                    case DOUBLE -> Double.parseDouble(text);
                    case INT -> Long.parseLong(text);
                    case BOOLEAN -> {
                        if (!text.equals("true") && !text.equals("false")) throw new NumberFormatException(text);
                        yield text.equals("true") ? 1 : 0;
                    }
                };
                if (parsed < min || parsed > max) return false;
                value = parsed;
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private final Path file;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Map<String, List<String>> sectionComments = new LinkedHashMap<>();
    private final Deque<String> sections = new ArrayDeque<>();
    private List<String> pendingComment = List.of();

    public TomlConfig(Path file) {
        this.file = file;
    }

    // ------------------------------------------------------------------ ConfigBuilder

    @Override
    public ConfigBuilder comment(String... lines) {
        pendingComment = List.of(lines);
        return this;
    }

    @Override
    public ConfigBuilder push(String section) {
        sections.addLast(section);
        sectionComments.put(currentSection(), pendingComment);
        pendingComment = List.of();
        return this;
    }

    @Override
    public ConfigBuilder pop() {
        sections.removeLast();
        return this;
    }

    @Override
    public DoubleSetting defineInRange(String name, double defaultValue, double min, double max) {
        Entry entry = add(name, Kind.DOUBLE, defaultValue, min, max);
        return () -> entry.value;
    }

    @Override
    public IntSetting defineInRange(String name, int defaultValue, int min, int max) {
        Entry entry = add(name, Kind.INT, defaultValue, min, max);
        return () -> (int) entry.value;
    }

    @Override
    public BooleanSetting define(String name, boolean defaultValue) {
        Entry entry = add(name, Kind.BOOLEAN, defaultValue ? 1 : 0, 0, 1);
        return () -> entry.value != 0;
    }

    private Entry add(String name, Kind kind, double defaultValue, double min, double max) {
        Entry entry = new Entry(currentSection(), name, pendingComment, kind, defaultValue, min, max);
        pendingComment = List.of();
        entries.put(entry.path(), entry);
        return entry;
    }

    private String currentSection() {
        return String.join(".", sections);
    }

    // ------------------------------------------------------------------ file

    /** Reads the file (if present), then writes it back complete with comments. */
    public void load() {
        entries.values().forEach(entry -> entry.value = entry.defaultValue);
        if (Files.exists(file)) {
            try {
                apply(parse(Files.readAllLines(file)));
            } catch (IOException e) {
                LOGGER.warn("Could not read {}, using defaults", file, e);
            }
        }
        save();
    }

    private static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        String section = "";
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).strip();
                continue;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) continue;
            String key = line.substring(0, equals).strip();
            values.put(section.isEmpty() ? key : section + "." + key, line.substring(equals + 1).strip());
        }
        return values;
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        String section = null;
        for (Entry entry : entries.values()) {
            if (!entry.section.equals(section)) {
                section = entry.section;
                if (!lines.isEmpty()) lines.add("");
                sectionComments.getOrDefault(section, List.of()).forEach(comment -> lines.add("# " + comment));
                if (!section.isEmpty()) lines.add("[" + section + "]");
            }
            entry.comment.forEach(comment -> lines.add("    # " + comment));
            String range = entry.kind == Kind.BOOLEAN ? "" : " (" + format(entry, entry.min) + " .. " + format(entry, entry.max) + ")";
            lines.add("    # Default: " + format(entry, entry.defaultValue) + range);
            lines.add("    " + entry.name + " = " + entry.format());
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines);
        } catch (IOException e) {
            LOGGER.warn("Could not write {}", file, e);
        }
    }

    private static String format(Entry entry, double value) {
        return switch (entry.kind) {
            case DOUBLE -> Double.toString(value);
            case INT -> Long.toString((long) value);
            case BOOLEAN -> Boolean.toString(value != 0);
        };
    }

    // ------------------------------------------------------------------ sync

    /** All values as text (the server sends them to joining clients). */
    public Map<String, String> values() {
        Map<String, String> values = new LinkedHashMap<>();
        entries.values().forEach(entry -> values.put(entry.path(), entry.format()));
        return values;
    }

    /** Applies values (from the file or the server); unknown keys and invalid values are ignored. */
    public void apply(Map<String, String> values) {
        values.forEach((path, text) -> {
            Entry entry = entries.get(path);
            if (entry != null && !entry.parse(text)) LOGGER.warn("Invalid value {} for {}, keeping {}", text, path, entry.format());
        });
    }
}
