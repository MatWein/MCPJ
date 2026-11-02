package de.mcpj.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TargetingConfig {
    private final ForgeConfigSpec.DoubleValue scanRange;
    private final ForgeConfigSpec.DoubleValue lockRange;
    private final ForgeConfigSpec.DoubleValue autoTargetRange;

    TargetingConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Target acquisition").push("targeting");
        scanRange = builder.comment("Range in which targets are marked and can be selected (blocks)")
                .defineInRange("scanRange", 64.0, 1.0, 256.0);
        lockRange = builder.comment("A selected target is dropped beyond this range (blocks)")
                .defineInRange("lockRange", 96.0, 1.0, 256.0);
        autoTargetRange = builder.comment("Range for automatic targeting when firing without selection (blocks)")
                .defineInRange("autoTargetRange", 40.0, 0.0, 256.0);
        builder.pop();
    }

    public double scanRange() {
        return scanRange.get();
    }

    public double lockRange() {
        return lockRange.get();
    }

    public double autoTargetRange() {
        return autoTargetRange.get();
    }
}
