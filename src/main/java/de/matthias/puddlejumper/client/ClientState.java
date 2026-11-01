package de.matthias.puddlejumper.client;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Clientseitiger Zustand: Zielerfassung, Drohnen-Cooldown-Anzeige, Triebwerkssounds. */
public final class ClientState {
    public static final double TARGET_RANGE = 96;
    @Nullable public static LivingEntity target;
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
        if (j.isVehicle() && (s == null || s.isStopped() || !s.isFor(j))) {
            s = new JumperEngineSound(j);
            SOUNDS.put(j.getId(), s);
            mc.getSoundManager().play(s);
        }
        SOUNDS.values().removeIf(JumperEngineSound::isStopped);
    }

    /** Zielerfassung entlang der Blickrichtung (mit etwas Zielhilfe). */
    public static void updateTarget(PuddleJumperEntity jumper) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) { target = null; return; }
        Vec3 from = p.getEyePosition();
        Vec3 dir = p.getViewVector(1f);
        Vec3 to = from.add(dir.scale(TARGET_RANGE));
        HitResult block = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) to = block.getLocation();
        double best = Double.MAX_VALUE;
        LivingEntity found = null;
        AABB search = new AABB(from, to).inflate(2.0);
        for (Entity e : p.level().getEntities(p, search, en -> en instanceof LivingEntity && en.isAlive() && !en.isSpectator() && en.getRootVehicle() != jumper)) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(1.0).clip(from, to);
            if (hit.isPresent()) {
                double d = from.distanceToSqr(hit.get());
                if (d < best) { best = d; found = (LivingEntity) e; }
            }
        }
        target = found;
    }
}
