package de.mcpj.core.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDefaultsTest {
    @Test
    void serverConfigIsConsistent() {
        DefaultConfigBuilder builder = new DefaultConfigBuilder();
        ServerConfig.define(builder);
        assertTrue(builder.problems.isEmpty(), builder.problems.toString());
        assertTrue(builder.sectionsClosed(), "a section is not closed");
        assertTrue(builder.settings > 50, "settings missing: " + builder.settings);
    }

    @Test
    void clientConfigIsConsistent() {
        DefaultConfigBuilder builder = new DefaultConfigBuilder();
        ClientConfig.define(builder);
        assertTrue(builder.problems.isEmpty(), builder.problems.toString());
        assertTrue(builder.sectionsClosed(), "a section is not closed");
    }

    @Test
    void documentedDefaults() {
        DefaultConfigBuilder.useDefaults();
        assertEquals(240f, ServerConfig.HULL.maxHull());
        assertEquals(8f, ServerConfig.HULL.lavaDamagePerSecond());
        assertEquals(60_000_000, ServerConfig.ENERGY.capacity());
        assertEquals(14.3, ClientConfig.cameraDistance(), 1e-9);
    }
}
