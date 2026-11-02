package de.matthias.puddlejumper;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-Einstellungen (world/serverconfig/puddlejumper-server.toml). Energie in Forge Energy (FE);
 * Mekanism rechnet automatisch um (1 FE = 2,5 J).
 */
public final class Config {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue CAPACITY;
    public static final ForgeConfigSpec.DoubleValue START_CHARGE;
    public static final ForgeConfigSpec.IntValue HOVER_DRAIN;
    public static final ForgeConfigSpec.IntValue THRUST_DRAIN;
    public static final ForgeConfigSpec.DoubleValue WATER_DRIVE_FACTOR;
    public static final ForgeConfigSpec.IntValue SHIELD_DRAIN;
    public static final ForgeConfigSpec.DoubleValue SHIELD_WATER_FACTOR;
    public static final ForgeConfigSpec.IntValue SHIELD_HIT_COST;
    public static final ForgeConfigSpec.IntValue CLOAK_DRAIN;
    public static final ForgeConfigSpec.DoubleValue CLOAK_WATER_FACTOR;
    public static final ForgeConfigSpec.DoubleValue UNDER_FIRE_FACTOR;
    public static final ForgeConfigSpec.IntValue DRONE_COST;
    public static final ForgeConfigSpec.IntValue LIGHT_DRAIN;
    public static final ForgeConfigSpec.IntValue PAD_CHARGE_RATE;
    public static final ForgeConfigSpec.IntValue PAD_INPUT_RATE;
    public static final ForgeConfigSpec.IntValue PAD_BUFFER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Energie des Puddle Jumpers (FE)").push("jumper");
        CAPACITY = b.comment("Speichergröße (60 Mio. FE = ~33 min Vollgas ohne weitere Verbraucher)").defineInRange("capacity", 60_000_000, 1_000, Integer.MAX_VALUE);
        START_CHARGE = b.comment("Ladung neuer (und bisher energieloser) Jumper, Anteil 0..1")
                .defineInRange("startCharge", 0.2, 0.0, 1.0);
        HOVER_DRAIN = b.comment("Antrieb in der Luft ohne Tempo (FE/Tick); gelandet: 0").defineInRange("hoverDrain", 250, 0, 1_000_000);
        THRUST_DRAIN = b.comment("Antrieb bei Höchsttempo (FE/Tick), dazwischen linear").defineInRange("thrustDrain", 1_500, 0, 1_000_000);
        WATER_DRIVE_FACTOR = b.comment("Antrieb unter Wasser: Faktor").defineInRange("waterDriveFactor", 1.5, 1.0, 100.0);
        SHIELD_DRAIN = b.comment("Schutzschild (FE/Tick)").defineInRange("shieldDrain", 500, 0, 1_000_000);
        SHIELD_WATER_FACTOR = b.comment("Schild unter Wasser: Faktor").defineInRange("shieldWaterFactor", 2.0, 1.0, 100.0);
        SHIELD_HIT_COST = b.comment("Schild: zusätzlich pro abgefangenem Schadenspunkt (FE)").defineInRange("shieldHitCost", 200, 0, 1_000_000);
        CLOAK_DRAIN = b.comment("Tarnung (FE/Tick)").defineInRange("cloakDrain", 400, 0, 1_000_000);
        CLOAK_WATER_FACTOR = b.comment("Tarnung unter Wasser: Faktor").defineInRange("cloakWaterFactor", 1.5, 1.0, 100.0);
        UNDER_FIRE_FACTOR = b.comment("Schild und Tarnung unter Beschuss (bis 2 s nach einem Treffer): Faktor")
                .defineInRange("underFireFactor", 2.0, 1.0, 100.0);
        DRONE_COST = b.comment("Pro abgefeuerter Drohne (FE)").defineInRange("droneCost", 150_000, 0, Integer.MAX_VALUE);
        LIGHT_DRAIN = b.comment("Licht (FE/Tick)").defineInRange("lightDrain", 20, 0, 1_000_000);
        b.pop();

        b.comment("Ladestation").push("chargingPad");
        PAD_CHARGE_RATE = b.comment("Ladeleistung pro Station unter dem gelandeten Jumper (FE/Tick)")
                .defineInRange("chargeRate", 10_000, 1, Integer.MAX_VALUE);
        PAD_INPUT_RATE = b.comment("Maximale Aufnahme aus Kabeln (FE/Tick)").defineInRange("inputRate", 20_000, 1, Integer.MAX_VALUE);
        PAD_BUFFER = b.comment("Interner Puffer (FE)").defineInRange("buffer", 200_000, 1, Integer.MAX_VALUE);
        b.pop();
        SPEC = b.build();
    }

    private Config() {}
}
