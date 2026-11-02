package de.mcpj.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnitsTest {
    @Test
    void convertsSpeedsAndDurations() {
        assertEquals(1.0, Units.perTick(20.0), 1e-12);
        assertEquals(20.0, Units.perSecond(1.0), 1e-12);
        assertEquals(0.022, Units.perTickSquared(8.8), 1e-12);
        assertEquals(40, Units.ticks(2.0));
        assertEquals(2, Units.ticks(0.12));
        assertEquals(3, Units.ticks(0.13));
    }
}
