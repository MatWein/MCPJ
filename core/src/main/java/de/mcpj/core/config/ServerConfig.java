package de.mcpj.core.config;

/**
 * All server settings (synced to clients). The platform module calls {@link #define} once while building its
 * config; afterwards the sections are available as static fields.
 */
public final class ServerConfig {
    public static FlightConfig FLIGHT;
    public static HullConfig HULL;
    public static EnergyConfig ENERGY;
    public static DroneConfig DRONES;
    public static TargetingConfig TARGETING;
    public static LightConfig LIGHTS;
    public static CargoConfig CARGO;
    public static ChargingPadConfig CHARGING_PAD;

    private ServerConfig() {}

    public static void define(ConfigBuilder builder) {
        FLIGHT = new FlightConfig(builder);
        HULL = new HullConfig(builder);
        ENERGY = new EnergyConfig(builder);
        DRONES = new DroneConfig(builder);
        TARGETING = new TargetingConfig(builder);
        LIGHTS = new LightConfig(builder);
        CARGO = new CargoConfig(builder);
        CHARGING_PAD = new ChargingPadConfig(builder);
    }
}
