package de.matthias.puddlejumper.client;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/** Clientseitiger Zustand: Zielerfassung (Tab), Drohnen-Cooldown-Anzeige, Triebwerkssounds. */
public final class ClientState {
    public static final double TARGET_RANGE = 96;
    @Nullable public static Entity target;
    public static long lastFireTick = -100;
    private static final Map<Integer, JumperEngineSound> SOUNDS = new HashMap<>();

    private ClientState() {}

    @Nullable
    public static PuddleJumperEntity pilotedJumper() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && p.getVehicle() instanceof PuddleJumperEntity j && j.getControllingPassenger() == p) return j;
        return null;
    }

    /** Pro Client-Tick jedes Jumpers aufgerufen. */
    public static void tickJumper(PuddleJumperEntity j) {
        JumperEngineSound s = SOUNDS.get(j.getId());
        Minecraft mc = Minecraft.getInstance();
        if (j.enginesRunning() && (s == null || s.isStopped() || !s.isFor(j))) {
            s = new JumperEngineSound(j);
            SOUNDS.put(j.getId(), s);
            mc.getSoundManager().play(s);
        }
        SOUNDS.values().removeIf(JumperEngineSound::isStopped);
    }

    /** Reichweite der Zielerfassung (Blöcke). */
    public static final double SCAN_RANGE = 64;
    /** Nach so vielen Ticks ohne Tab beginnt die Auswahl wieder beim nächstgelegenen Ziel. */
    private static final int CYCLE_RESET_TICKS = 60;
    private static final Set<Integer> VISITED = new HashSet<>();
    private static long lastCycleTick = -1000;

    /** Erfassbare Ziele um den Jumper (Lebewesen und andere Jumper), nächstgelegene zuerst. */
    public static List<Entity> candidates(PuddleJumperEntity jumper) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return List.of();
        Vec3 c = jumper.position();
        List<Entity> list = p.level().getEntities(jumper, jumper.getBoundingBox().inflate(SCAN_RANGE),
                e -> e != p && jumper.isValidTarget(e) && distanceSq(e, c) <= SCAN_RANGE * SCAN_RANGE);
        list.sort(Comparator.comparingDouble(e -> distanceSq(e, c)));
        return list;
    }

    /** Abstand zur nächsten Stelle der Hitbox (bei Jumpern: ganzes Schiff). */
    public static double distanceSq(Entity e, Vec3 from) {
        AABB box = e instanceof PuddleJumperEntity j ? j.fullBoundingBox() : e.getBoundingBox();
        double dx = Math.max(0, Math.max(box.minX - from.x, from.x - box.maxX));
        double dy = Math.max(0, Math.max(box.minY - from.y, from.y - box.maxY));
        double dz = Math.max(0, Math.max(box.minZ - from.z, from.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Tab: immer das nächstgelegene Ziel, das in dieser Runde noch nicht gewählt war. Sind alle durch -> kein Ziel.
     * (Merkt sich die gewählten Ziele, damit sich bewegende Mobs die Reihenfolge nicht durcheinanderbringen.)
     */
    public static void cycleTarget(PuddleJumperEntity jumper) {
        long now = jumper.level().getGameTime();
        if (now - lastCycleTick > CYCLE_RESET_TICKS) VISITED.clear();
        lastCycleTick = now;
        if (target != null) VISITED.add(target.getId());
        Entity next = null;
        for (Entity e : candidates(jumper)) {
            if (!VISITED.contains(e.getId())) {
                next = e;
                break;
            }
        }
        if (next == null) VISITED.clear();
        target = next;
    }

    /** Ziel verwerfen, wenn es tot, weg, getarnt oder außer Reichweite ist. */
    public static void validateTarget(PuddleJumperEntity jumper) {
        if (target != null && (!jumper.isValidTarget(target) || target.level() != jumper.level()
                || distanceSq(target, jumper.position()) > TARGET_RANGE * TARGET_RANGE)) {
            target = null;
        }
    }
}
