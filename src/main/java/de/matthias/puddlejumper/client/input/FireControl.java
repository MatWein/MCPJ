package de.matthias.puddlejumper.client.input;

import de.matthias.puddlejumper.config.ServerConfig;
import net.minecraft.world.level.Level;

public final class FireControl {
    private static final long NEVER = -1_000_000L;
    private static long lastFireTick = NEVER;

    private FireControl() {}

    public static boolean isReady(Level level) {
        return level.getGameTime() - lastFireTick >= ServerConfig.DRONES.cooldownTicks();
    }

    static boolean tryFire(Level level) {
        if (!isReady(level)) return false;
        lastFireTick = level.getGameTime();
        return true;
    }
}
