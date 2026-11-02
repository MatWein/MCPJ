package de.mcpj.core.config;

import de.mcpj.core.util.Units;

public final class LightConfig {
    private static final int MAX_LIGHT_LEVEL = 15;

    private final IntSetting lightLevel;
    private final DoubleSetting moveDistance;
    private final IntSetting updateIntervalTicks;
    private final DoubleSetting singleLightSpeed;

    LightConfig(ConfigBuilder builder) {
        builder.comment("Headlights (invisible vanilla light blocks that follow the jumper)").push("lights");
        lightLevel = builder.comment("Light level of the headlights")
                .defineInRange("lightLevel", MAX_LIGHT_LEVEL, 1, MAX_LIGHT_LEVEL);
        moveDistance = builder.comment("A light is moved once the jumper is this far away from it (blocks)")
                .defineInRange("moveDistance", 2.0, 0.0, 16.0);
        updateIntervalTicks = builder.comment("Minimum ticks between two light moves")
                .defineInRange("updateIntervalTicks", 4, 1, 100);
        singleLightSpeed = builder.comment("Above this speed only the bow light is used (blocks per second)")
                .defineInRange("singleLightSpeed", 10.0, 0.0, 100.0);
        builder.pop();
    }

    public int lightLevel() {
        return lightLevel.get();
    }

    public double moveDistance() {
        return moveDistance.get();
    }

    public int updateIntervalTicks() {
        return updateIntervalTicks.get();
    }

    public double singleLightSpeedPerTick() {
        return Units.perTick(singleLightSpeed.get());
    }
}
