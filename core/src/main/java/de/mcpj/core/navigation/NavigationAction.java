package de.mcpj.core.navigation;

/** Changes to the navigation memory a passenger can make in the navigation console. */
public enum NavigationAction {
    /** Saves the jumper's current position under the given name. */
    ADD,
    /** Gives the waypoint at the index a new name. */
    RENAME,
    DELETE,
    /** Makes the waypoint at the index the destination ({@link JumperNavigation#NONE} clears it). */
    SELECT
}
