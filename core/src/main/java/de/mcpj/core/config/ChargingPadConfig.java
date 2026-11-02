package de.mcpj.core.config;

public final class ChargingPadConfig {
    private final IntSetting chargeRate;
    private final IntSetting inputRate;
    private final IntSetting buffer;

    ChargingPadConfig(ConfigBuilder builder) {
        builder.comment("Jumper charging pad").push("chargingPad");
        chargeRate = builder.comment("Charging power per pad below a landed jumper (FE/tick)")
                .defineInRange("chargeRate", 10_000, 1, Integer.MAX_VALUE);
        inputRate = builder.comment("Maximum input from cables (FE/tick)")
                .defineInRange("inputRate", 20_000, 1, Integer.MAX_VALUE);
        buffer = builder.comment("Internal buffer (FE)")
                .defineInRange("buffer", 200_000, 1, Integer.MAX_VALUE);
        builder.pop();
    }

    public int chargeRate() {
        return chargeRate.get();
    }

    public int inputRate() {
        return inputRate.get();
    }

    public int buffer() {
        return buffer.get();
    }
}
