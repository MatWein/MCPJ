package de.mcpj.core.targeting;

import de.mcpj.core.math.Box;
import de.mcpj.core.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TargetCycleTest {
    @Test
    void cyclesNearestFirstThenNoneThenStartsOver() {
        TargetCycle cycle = new TargetCycle();
        List<Integer> candidates = List.of(7, 3, 9);
        assertEquals(7, cycle.next(100, null, candidates));
        assertEquals(3, cycle.next(101, 7, candidates));
        assertEquals(9, cycle.next(102, 3, candidates));
        assertNull(cycle.next(103, 9, candidates));
        assertEquals(7, cycle.next(104, null, candidates));
    }

    @Test
    void pauseResetsTheCycle() {
        TargetCycle cycle = new TargetCycle();
        List<Integer> candidates = List.of(7, 3);
        assertEquals(7, cycle.next(100, null, candidates));
        assertEquals(3, cycle.next(101, 7, candidates));
        // after more than 3 seconds the nearest one comes first again
        assertEquals(7, cycle.next(200, null, candidates));
    }

    @Test
    void distanceToTheNearestPointOfABox() {
        Box box = new Box(0, 0, 0, 2, 2, 2);
        assertEquals(0, TargetCycle.distanceSqr(box, new Vec3d(1, 1, 1)));
        assertEquals(9, TargetCycle.distanceSqr(box, new Vec3d(5, 1, 1)), 1e-9);
    }
}
