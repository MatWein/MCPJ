package de.matthias.puddlejumper.util;

public final class Units {
    public static final int TICKS_PER_SECOND = 20;

    private Units() {}

    public static double perTick(double perSecond) {
        return perSecond / TICKS_PER_SECOND;
    }

    public static double perTickSquared(double perSecondSquared) {
        return perSecondSquared / (TICKS_PER_SECOND * TICKS_PER_SECOND);
    }

    public static double perSecond(double perTick) {
        return perTick * TICKS_PER_SECOND;
    }

    public static int ticks(double seconds) {
        return (int) Math.round(seconds * TICKS_PER_SECOND);
    }
}
