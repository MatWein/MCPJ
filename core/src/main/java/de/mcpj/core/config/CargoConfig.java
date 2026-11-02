package de.mcpj.core.config;

public final class CargoConfig {
    private final BooleanSetting autoPickup;
    private final DoubleSetting pickupRange;
    private final IntSetting pickupIntervalTicks;

    CargoConfig(ConfigBuilder builder) {
        builder.comment("Cargo hold").push("cargo");
        autoPickup = builder.comment("Collect dropped items close to the hull into the cargo hold")
                .define("autoPickup", true);
        pickupRange = builder.comment("Pickup range around the hull (blocks)")
                .defineInRange("pickupRange", 1.0, 0.0, 8.0);
        pickupIntervalTicks = builder.comment("Ticks between two pickup scans")
                .defineInRange("pickupIntervalTicks", 4, 1, 100);
        builder.pop();
    }

    public boolean autoPickup() {
        return autoPickup.get();
    }

    public double pickupRange() {
        return pickupRange.get();
    }

    public int pickupIntervalTicks() {
        return pickupIntervalTicks.get();
    }
}
