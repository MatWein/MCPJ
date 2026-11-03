package de.mcpj.mc.compat;

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
import org.joml.Vector3f;

/**
 * Small Minecraft API differences, so that shared code (mc_core_common) compiles against every version. Each
 * mc_&lt;version&gt;_common has this class with the same methods; client-only calls are in ClientCompat.
 */
public final class McCompat {
    private McCompat() {}

    /** Seats the passenger even if the vehicle would not normally accept it. */
    public static boolean ride(Entity passenger, Entity vehicle) {
        return passenger.startRiding(vehicle, true);
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        player.teleportTo(level, x, y, z, yaw, pitch);
    }

    public static Packet<?> vehicleMovePacket(Entity vehicle) {
        return new ClientboundMoveVehiclePacket(vehicle);
    }

    public static SoundEvent explodeSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    public static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f), scale);
    }

    /** Hits the entity on the server (Minecraft's hurt). */
    public static boolean hurt(Entity entity, DamageSource source, float amount) {
        return entity.hurt(source, amount);
    }

    public static int invulnerableTime(Entity entity) {
        return entity.invulnerableTime;
    }

    public static void setInvulnerableTime(Entity entity, int ticks) {
        entity.invulnerableTime = ticks;
    }

    /** A mob's melee attack on its target: swing and hit. */
    public static void meleeAttack(Mob mob, Entity target) {
        mob.swing(InteractionHand.MAIN_HAND);
        mob.doHurtTarget(target);
    }
}
