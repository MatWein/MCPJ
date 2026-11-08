package de.mcpj.core.jumper;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumperLockTest {
    private final UUID ship = UUID.randomUUID();
    private final JumperLock lock = new JumperLock(new FakeJumperHost());

    @Test
    void newShipIsOpen() {
        assertFalse(lock.locked());
    }

    @Test
    void blankKeyPairsAndLocks() {
        assertEquals(JumperLock.KeyResult.PAIRED, lock.useKey(null, ship));
        assertTrue(lock.locked());
    }

    @Test
    void pairedKeyTogglesTheLock() {
        lock.useKey(null, ship);
        assertEquals(JumperLock.KeyResult.UNLOCKED, lock.useKey(ship, ship));
        assertFalse(lock.locked());
        assertEquals(JumperLock.KeyResult.LOCKED, lock.useKey(ship, ship));
        assertTrue(lock.locked());
    }

    @Test
    void secondBlankKeyCannotClaimAPairedShip() {
        lock.useKey(null, ship);
        lock.useKey(ship, ship);
        assertEquals(JumperLock.KeyResult.TAKEN, lock.useKey(null, ship));
        assertFalse(lock.locked());
    }

    @Test
    void keyOfAnotherShipChangesNothing() {
        lock.useKey(null, ship);
        assertEquals(JumperLock.KeyResult.WRONG_JUMPER, lock.useKey(UUID.randomUUID(), ship));
        assertTrue(lock.locked());
    }
}
