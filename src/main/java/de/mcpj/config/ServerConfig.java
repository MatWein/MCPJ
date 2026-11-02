package de.mcpj.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final FlightConfig FLIGHT;
    public static final HullConfig HULL;
    public static final EnergyConfig ENERGY;
    public static final DroneConfig DRONES;
    public static final TargetingConfig TARGETING;
    public static final LightConfig LIGHTS;
    public static final CargoConfig CARGO;
    public static final ChargingPadConfig CHARGING_PAD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        FLIGHT = new FlightConfig(builder);
        HULL = new HullConfig(builder);
        ENERGY = new EnergyConfig(builder);
        DRONES = new DroneConfig(builder);
        TARGETING = new TargetingConfig(builder);
        LIGHTS = new LightConfig(builder);
        CARGO = new CargoConfig(builder);
        CHARGING_PAD = new ChargingPadConfig(builder);
        SPEC = builder.build();
    }

    private ServerConfig() {}
}
