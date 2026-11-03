package de.mcpj.mc.compat;

import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.Level;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Set;

/**
 * Small Minecraft API differences, so that shared code (mc_core_common) compiles against every version. Each
 * mc_&lt;version&gt;_common has this class with the same methods; client-only calls are in ClientCompat.
 */
public final class McCompat {
    private McCompat() {}

    /** Seats the passenger even if the vehicle would not normally accept it. */
    public static boolean ride(Entity passenger, Entity vehicle) {
        return passenger.startRiding(vehicle, true, false);
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        player.teleportTo(level, x, y, z, Set.of(), yaw, pitch, false);
    }

    public static Packet<?> vehicleMovePacket(Entity vehicle) {
        return ClientboundMoveVehiclePacket.fromEntity(vehicle);
    }

    public static SoundEvent explodeSound() {
        return SoundEvents.GENERIC_EXPLODE.value();
    }

    public static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(rgb, scale);
    }

    /** Hits the entity on the server (Minecraft's hurtServer). */
    public static boolean hurt(Entity entity, DamageSource source, float amount) {
        return entity.level() instanceof ServerLevel level && entity.hurtServer(level, source, amount);
    }

    public static int invulnerableTime(Entity entity) {
        return entity.getInvulnerableTime();
    }

    public static void setInvulnerableTime(Entity entity, int ticks) {
        entity.setInvulnerableTime(ticks);
    }

    /** A mob's melee attack on its target: swing and hit. */
    public static void meleeAttack(Mob mob, Entity target) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        mob.swingForAttack(InteractionHand.MAIN_HAND);
        mob.doHurtTarget(level, target);
    }

    /** A new jumper at that spot and heading (not yet in the world); null if it could not be created. */
    @Nullable
    public static PuddleJumperEntity newJumper(Level level, double x, double y, double z, float yaw) {
        PuddleJumperEntity jumper = McpjPlatform.get().jumperType().create(level, EntitySpawnReason.TRIGGERED);
        if (jumper != null) jumper.snapTo(x, y, z, yaw, 0f);
        return jumper;
    }
}
