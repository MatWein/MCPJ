package de.mcpj.core.config;

/** Client-only settings. The platform module calls {@link #define} once while building its client config. */
public final class ClientConfig {
    private static DoubleSetting cameraDistance;
    private static DoubleSetting lowEnergyWarning;

    private ClientConfig() {}

    public static void define(ConfigBuilder builder) {
        builder.comment("Client settings of the Puddle Jumper").push("client");
        cameraDistance = builder.comment("Third person camera distance while piloting (vanilla: 4)")
                .defineInRange("cameraDistance", 14.3, 4.0, 64.0);
        lowEnergyWarning = builder.comment("Energy share below which the HUD blinks and a warning sounds")
                .defineInRange("lowEnergyWarning", 0.1, 0.0, 1.0);
        builder.pop();
    }

    public static double cameraDistance() {
        return cameraDistance.get();
    }

    public static float lowEnergyWarning() {
        return (float) lowEnergyWarning.get();
    }
}
