package de.mcpj.mc.entity.jumper;

import de.mcpj.core.jumper.JumperEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/**
 * Lightning charges the jumper instead of hurting it, and during thunderstorms the jumper attracts lightning like a
 * lightning rod (smaller range).
 */
public final class JumperLightning {
    /** Horizontal range in which a jumper under open sky draws natural lightning (a lightning rod: 128). */
    public static final double ATTRACT_RANGE = 32;
    private static final double ATTRACT_HEIGHT = 512;
    private static final float CHARGE_FRACTION = 0.2f;

    private final PuddleJumperEntity jumper;
    @Nullable private UUID lastBolt;

    JumperLightning(PuddleJumperEntity jumper) {
        this.jumper = jumper;
    }

    /** Struck by lightning (also via a hull segment): 20 % charge once per bolt, no damage, no fire. */
    public void struck(LightningBolt bolt) {
        if (bolt.getUUID().equals(lastBolt)) return;
        lastBolt = bolt.getUUID();
        JumperEnergy energy = jumper.energy();
        long charged = energy.stored() + (long) (energy.capacity() * (double) CHARGE_FRACTION);
        energy.set((int) Math.min(charged, energy.capacity()));
    }

    /** A lightning bolt enters the world: natural lightning in a thunderstorm strikes the nearest jumper in range. */
    public static void attract(LightningBolt bolt) {
        if (bolt.getCause() == null && bolt.level() instanceof ServerLevel level && level.isThundering()) strikeNearestJumper(bolt);
    }

    /** Moves the bolt onto the nearest jumper under open sky within {@link #ATTRACT_RANGE} (on top of it: no fire below). */
    public static void strikeNearestJumper(LightningBolt bolt) {
        AABB area = new AABB(bolt.position(), bolt.position()).inflate(ATTRACT_RANGE, ATTRACT_HEIGHT, ATTRACT_RANGE);
        bolt.level().getEntitiesOfClass(PuddleJumperEntity.class, area, JumperLightning::underOpenSky).stream()
                .min(Comparator.comparingDouble(jumper -> horizontalDistanceSqr(jumper, bolt)))
                .ifPresent(jumper -> bolt.setPos(jumper.getX(), jumper.getBoundingBox().maxY, jumper.getZ()));
    }

    private static boolean underOpenSky(PuddleJumperEntity jumper) {
        return jumper.isAlive() && jumper.level().canSeeSky(BlockPos.containing(jumper.getX(), jumper.getBoundingBox().maxY, jumper.getZ()));
    }

    private static double horizontalDistanceSqr(PuddleJumperEntity jumper, LightningBolt bolt) {
        double dx = jumper.getX() - bolt.getX();
        double dz = jumper.getZ() - bolt.getZ();
        return dx * dx + dz * dz;
    }
}
