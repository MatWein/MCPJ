package de.mcpj.core.navigation;

import de.mcpj.core.jumper.JumperSync;
import de.mcpj.core.math.BlockPosition;
import de.mcpj.core.util.SaveData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The jumper's navigation memory: named waypoints and the current destination. It belongs to the ship (saved with
 * it, the same for everyone on board). The server changes it; clients read it from the synced text
 * ({@link JumperSync#navigation()}), which is also what is saved.
 */
public final class JumperNavigation {
    public static final int NONE = -1;
    public static final int MAX_WAYPOINTS = 64;
    public static final int MAX_NAME_LENGTH = 32;
    private static final String SAVE_TAG = "Navigation";
    private static final char FIELD = '\t';
    private static final char LINE = '\n';

    private final JumperSync sync;
    private String decodedFrom = "";
    private List<Waypoint> waypoints = List.of();
    private int selected = NONE;

    public JumperNavigation(JumperSync sync) {
        this.sync = sync;
    }

    public List<Waypoint> waypoints() {
        decode();
        return waypoints;
    }

    /** Index of the destination, or {@link #NONE}. */
    public int selectedIndex() {
        decode();
        return selected;
    }

    @Nullable
    public Waypoint destination() {
        decode();
        return selected == NONE ? null : waypoints.get(selected);
    }

    public boolean isFull() {
        return waypoints().size() >= MAX_WAYPOINTS;
    }

    // ------------------------------------------------------------------ changes (server)

    /** Applies a change from the navigation console; invalid requests (index, full memory) are ignored. */
    public void apply(NavigationAction action, int index, String name, BlockPosition position, String dimension) {
        List<Waypoint> list = new ArrayList<>(waypoints());
        int destination = selectedIndex();
        boolean valid = index >= 0 && index < list.size();
        switch (action) {
            case ADD -> {
                if (list.size() >= MAX_WAYPOINTS) return;
                list.add(new Waypoint(cleanName(name, list.size() + 1), position.x(), position.y(), position.z(), dimension));
            }
            case RENAME -> {
                if (!valid) return;
                Waypoint old = list.get(index);
                list.set(index, new Waypoint(cleanName(name, index + 1), old.x(), old.y(), old.z(), old.dimension()));
            }
            case DELETE -> {
                if (!valid) return;
                list.remove(index);
                if (destination == index) destination = NONE;
                else if (destination > index) destination--;
            }
            case SELECT -> {
                if (!valid && index != NONE) return;
                destination = index;
            }
        }
        store(list, destination);
    }

    /** Trimmed, without tabs or line breaks, at most MAX_NAME_LENGTH characters; empty names get a number. */
    static String cleanName(String name, int number) {
        StringBuilder clean = new StringBuilder();
        for (char c : name.trim().toCharArray()) {
            if (!Character.isISOControl(c)) clean.append(c);
        }
        String result = clean.toString().trim();
        if (result.length() > MAX_NAME_LENGTH) result = result.substring(0, MAX_NAME_LENGTH).trim();
        return result.isEmpty() ? "#" + number : result;
    }

    private void store(List<Waypoint> list, int destination) {
        sync.setNavigation(encode(list, destination));
    }

    // ------------------------------------------------------------------ text form (sync and save)

    /** First line: index of the destination; then one waypoint per line: name, x, y, z, dimension (tab separated). */
    static String encode(List<Waypoint> list, int destination) {
        if (list.isEmpty()) return "";
        StringBuilder text = new StringBuilder().append(destination);
        for (Waypoint waypoint : list) {
            text.append(LINE).append(waypoint.name())
                    .append(FIELD).append(waypoint.x()).append(FIELD).append(waypoint.y()).append(FIELD).append(waypoint.z())
                    .append(FIELD).append(waypoint.dimension());
        }
        return text.toString();
    }

    private void decode() {
        String text = sync.navigation();
        if (text.equals(decodedFrom)) return;
        decodedFrom = text;
        List<Waypoint> list = new ArrayList<>();
        int destination = NONE;
        String[] lines = text.split(String.valueOf(LINE));
        try {
            if (!text.isEmpty()) destination = Integer.parseInt(lines[0].trim());
            for (int i = 1; i < lines.length; i++) {
                String[] fields = lines[i].split(String.valueOf(FIELD), -1);
                if (fields.length != 5) continue;
                list.add(new Waypoint(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                        Integer.parseInt(fields[3]), fields[4]));
            }
        } catch (NumberFormatException ignored) {
            // damaged text: keep what could be read
        }
        waypoints = Collections.unmodifiableList(list);
        selected = destination >= 0 && destination < list.size() ? destination : NONE;
    }

    public void load(SaveData data) {
        if (data.contains(SAVE_TAG)) sync.setNavigation(data.getString(SAVE_TAG));
    }

    public void save(SaveData data) {
        String text = sync.navigation();
        if (!text.isEmpty()) data.putString(SAVE_TAG, text);
    }
}
