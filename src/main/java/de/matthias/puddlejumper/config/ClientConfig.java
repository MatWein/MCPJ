package de.matthias.puddlejumper.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue CAMERA_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue LOW_ENERGY_WARNING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Client settings of the Puddle Jumper").push("client");
        CAMERA_DISTANCE = builder.comment("Third person camera distance while piloting (vanilla: 4)")
                .defineInRange("cameraDistance", 14.3, 4.0, 64.0);
        LOW_ENERGY_WARNING = builder.comment("Energy share below which the HUD blinks and a warning sounds")
                .defineInRange("lowEnergyWarning", 0.1, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}

    public static double cameraDistance() {
        return CAMERA_DISTANCE.get();
    }

    public static float lowEnergyWarning() {
        return LOW_ENERGY_WARNING.get().floatValue();
    }
}
