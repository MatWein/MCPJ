package de.matthias.puddlejumper.entity.jumper;

import java.util.function.Supplier;

/** Read on the pilot's client only; the flight model runs there (like boats). */
public record FlightInput(float forward, float turn, float vertical, float strafe) {
    public static final FlightInput NONE = new FlightInput(0, 0, 0, 0);
    public static Supplier<FlightInput> source = () -> NONE;

    public boolean hasThrust() {
        return forward != 0 || vertical != 0 || strafe != 0;
    }
}
