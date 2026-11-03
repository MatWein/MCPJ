package de.mcpj.core.config;

public final class ChargingPadConfig {
    private final IntSetting chargeRate;
    private final IntSetting inputRate;
    private final IntSetting buffer;
    private final BooleanSetting requirePower;
    private final IntSetting freeChargeRate;

    ChargingPadConfig(ConfigBuilder builder) {
        builder.comment("Jumper charging pad").push("chargingPad");
        chargeRate = builder.comment("Charging power per pad below a landed jumper (FE/tick)")
                .defineInRange("chargeRate", 10_000, 1, Integer.MAX_VALUE);
        inputRate = builder.comment("Maximum input from cables (FE/tick)")
                .defineInRange("inputRate", 20_000, 1, Integer.MAX_VALUE);
        buffer = builder.comment("Internal buffer (FE)")
                .defineInRange("buffer", 200_000, 1, Integer.MAX_VALUE);
        requirePower = builder.comment("Whether pads need energy from cables (energy mods) to charge a jumper. If false, a pad",
                        "charges on its own at freeChargeRate; energy from cables is still used first, at the full chargeRate")
                .define("requirePower", false);
        freeChargeRate = builder.comment("Charging power per pad without energy from cables, if requirePower is false (FE/tick)")
                .defineInRange("freeChargeRate", 2_500, 1, Integer.MAX_VALUE);
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

    public boolean requirePower() {
        return requirePower.get();
    }

    public int freeChargeRate() {
        return freeChargeRate.get();
    }
}
