package de.mcpj.core.jumper;

import de.mcpj.core.util.SaveData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The ship's lock. A blank key is paired with the ship on first use, which also locks it; from then on only that key
 * opens and closes it. The key itself remembers its ship; the ship only knows that it has been paired, so that no
 * second key can claim it. Clients read the locked state from {@link JumperSync#locked()}.
 */
public final class JumperLock {
    /** What using a key on the ship did. */
    public enum KeyResult {
        PAIRED,
        LOCKED,
        UNLOCKED,
        WRONG_JUMPER,
        TAKEN
    }

    private static final String PAIRED_TAG = "KeyPaired";
    private static final String LOCKED_TAG = "Locked";

    private final JumperSync sync;
    private boolean paired;

    public JumperLock(JumperSync sync) {
        this.sync = sync;
    }

    public boolean locked() {
        return sync.locked();
    }

    /**
     * Server: a key is used on the ship.
     *
     * @param keyJumper the ship the key is paired with, null for a blank key
     * @param jumper    this ship's id
     */
    public KeyResult useKey(@Nullable UUID keyJumper, UUID jumper) {
        if (keyJumper == null) {
            if (paired) return KeyResult.TAKEN;
            paired = true;
            sync.setLocked(true);
            return KeyResult.PAIRED;
        }
        if (!keyJumper.equals(jumper)) return KeyResult.WRONG_JUMPER;
        sync.setLocked(!sync.locked());
        return sync.locked() ? KeyResult.LOCKED : KeyResult.UNLOCKED;
    }

    public void load(SaveData data) {
        paired = data.getBoolean(PAIRED_TAG);
        sync.setLocked(data.getBoolean(LOCKED_TAG));
    }

    public void save(SaveData data) {
        data.putBoolean(PAIRED_TAG, paired);
        data.putBoolean(LOCKED_TAG, sync.locked());
    }
}
