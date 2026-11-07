package de.mcpj.core.navigation;

import de.mcpj.core.jumper.JumperSync;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumperNavigationTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final BlockPosition ORIGIN = new BlockPosition(0, 0, 0);
    private final String[] synced = {""};
    /** Only navigation()/setNavigation() of the sync are used. */
    private final JumperSync sync = (JumperSync) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{JumperSync.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "navigation" -> synced[0];
                case "setNavigation" -> {
                    synced[0] = (String) args[0];
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
    private final JumperNavigation navigation = new JumperNavigation(sync);

    private void add(String name, int x) {
        navigation.apply(NavigationAction.ADD, JumperNavigation.NONE, name, new BlockPosition(x, 64, 0), OVERWORLD);
    }

    @Test
    void addSelectRenameDelete() {
        add("Home", 0);
        add("Mine", 100);
        add("Village", 200);
        assertEquals(3, navigation.waypoints().size());
        assertNull(navigation.destination());

        navigation.apply(NavigationAction.SELECT, 2, "", ORIGIN, OVERWORLD);
        assertEquals("Village", navigation.destination().name());

        navigation.apply(NavigationAction.RENAME, 2, "  Big village ", ORIGIN, OVERWORLD);
        assertEquals("Big village", navigation.destination().name());
        assertEquals(200, navigation.destination().x(), "renaming keeps the position");

        // deleting an earlier waypoint keeps the destination
        navigation.apply(NavigationAction.DELETE, 0, "", ORIGIN, OVERWORLD);
        assertEquals("Big village", navigation.destination().name());
        navigation.apply(NavigationAction.DELETE, 1, "", ORIGIN, OVERWORLD);
        assertNull(navigation.destination(), "deleting the destination clears it");
        assertEquals(1, navigation.waypoints().size());
    }

    @Test
    void namesAreCleanedAndTheTextFormSurvives() {
        add("A\tB\nC", 0);
        add("", 1);
        add("x".repeat(50), 2);
        assertEquals("ABC", navigation.waypoints().get(0).name());
        assertEquals("#2", navigation.waypoints().get(1).name());
        assertEquals(JumperNavigation.MAX_NAME_LENGTH, navigation.waypoints().get(2).name().length());

        JumperNavigation reloaded = new JumperNavigation(sync);
        assertEquals(navigation.waypoints(), reloaded.waypoints());
    }

    @Test
    void invalidRequestsAreIgnoredAndMemoryIsLimited() {
        navigation.apply(NavigationAction.SELECT, 5, "", ORIGIN, OVERWORLD);
        navigation.apply(NavigationAction.DELETE, 0, "", ORIGIN, OVERWORLD);
        assertEquals("", synced[0]);
        for (int i = 0; i < JumperNavigation.MAX_WAYPOINTS + 3; i++) add("P" + i, i);
        assertEquals(JumperNavigation.MAX_WAYPOINTS, navigation.waypoints().size());
        assertTrue(navigation.isFull());
    }

    @Test
    void compassBearingsAndMarks() {
        Vec3d origin = Vec3d.ZERO;
        assertEquals(0f, Compass.bearing(origin, new Vec3d(0, 0, -10)), 1e-4);
        assertEquals(90f, Compass.bearing(origin, new Vec3d(10, 0, 0)), 1e-4);
        assertEquals(180f, Compass.bearing(origin, new Vec3d(0, 0, 10)), 1e-4);
        // Minecraft yaw: 0 = south, 90 = west
        assertEquals(180f, Compass.heading(0f), 1e-4);
        assertEquals(270f, Compass.heading(90f), 1e-4);
        assertEquals(-90f, Compass.relative(0f, 90f), 1e-4);
        assertEquals(20f, Compass.relative(10f, 350f), 1e-4);

        var marks = Compass.marks(350f, 30f);
        assertEquals(-20f, marks.get(0).offset(), 1e-4);
        assertTrue(marks.stream().anyMatch(mark -> "n".equals(mark.direction()) && Math.abs(mark.offset() - 10f) < 1e-4));
    }
}
