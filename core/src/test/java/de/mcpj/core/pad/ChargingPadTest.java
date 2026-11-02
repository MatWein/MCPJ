package de.mcpj.core.pad;

import de.mcpj.core.config.DefaultConfigBuilder;
import de.mcpj.core.config.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChargingPadTest {
    private static class Host implements ChargingPadHost {
        PadMode mode = PadMode.OFF;
        @Override public void markChanged() {}
        @Override public void comparatorChanged() {}
        @Override public PadMode currentMode() { return mode; }
        @Override public void setMode(PadMode mode) { this.mode = mode; }
    }

    @BeforeEach
    void setUp() {
        DefaultConfigBuilder.useDefaults();
    }

    @Test
    void buffersInputHandsItToJumpersAndShowsItsMode() {
        Host host = new Host();
        ChargingPad pad = new ChargingPad(host);
        int accepted = pad.receive(Integer.MAX_VALUE, false);
        assertEquals(Math.min(ServerConfig.CHARGING_PAD.inputRate(), pad.capacity()), accepted);
        pad.serverTick(5);
        assertEquals(PadMode.POWERED, host.mode);
        int supplied = pad.supplyJumper(Integer.MAX_VALUE, 5, 0.5f);
        assertEquals(Math.min(accepted, ServerConfig.CHARGING_PAD.chargeRate() * 5), supplied);
        assertEquals(Math.round(0.5f * 15), pad.comparatorLevel());
        pad.serverTick(10);
        assertEquals(PadMode.CHARGING, host.mode);
    }
}
