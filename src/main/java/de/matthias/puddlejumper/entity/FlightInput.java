package de.matthias.puddlejumper.entity;

import java.util.function.Supplier;

/**
 * Steuereingaben des Piloten. Wird nur auf dem Client des Piloten gelesen (dort läuft die
 * Flugphysik, wie bei Booten); der Client setzt {@link #source} beim Start.
 */
public record FlightInput(float forward, float turn, float vertical) {
    public static final FlightInput NONE = new FlightInput(0, 0, 0);
    public static Supplier<FlightInput> source = () -> NONE;

    public boolean idle() {
        return forward == 0 && turn == 0 && vertical == 0;
    }
}
