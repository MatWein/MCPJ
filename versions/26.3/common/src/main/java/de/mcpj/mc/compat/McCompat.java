package de.mcpj.mc.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import de.mcpj.mc.Mcpj;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.Level;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
 * Small Minecraft API differences, so that shared code (shared/mc/all) compiles against every version. Each
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

    /** The result of a successful use or interaction. */
    public static InteractionResult success(Level level) {
        return InteractionResult.SUCCESS;
    }

    /** A message above the hotbar. */
    public static void overlayMessage(Player player, Component message) {
        player.sendOverlayMessage(message);
    }

    /** Sends the packet to every player who sees the entity. */
    public static void sendToTracking(ServerLevel level, Entity entity, Packet<? super ClientGamePacketListener> packet) {
        level.getChunkSource().sendToTrackingPlayers(entity, packet);
    }

    // ------------------------------------------------------------------ building blocks, items and types (McpjObjects)

    /** Properties of an MCPJ block (Minecraft 26.3 wants the block's id up front). */
    public static BlockBehaviour.Properties blockProperties(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Mcpj.id(name)));
    }

    public static Item.Properties itemProperties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Mcpj.id(name)));
    }

    /** Properties of a block's item (named like the block). */
    public static Item.Properties blockItemProperties(String name) {
        return itemProperties(name).useBlockDescriptionPrefix();
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        return new BlockEntityType<>(factory, Set.of(block));
    }

    public static <T extends Entity> EntityType<T> entityType(EntityType.Builder<T> builder, String name) {
        return builder.build(ResourceKey.create(Registries.ENTITY_TYPE, Mcpj.id(name)));
    }

    public static <T extends Entity> EntityType.Builder<T> eyeHeight(EntityType.Builder<T> builder, float height) {
        return builder.eyeHeight(height);
    }

    /** Writes the sound's id (for network messages). */
    public static void writeSound(FriendlyByteBuf buffer, SoundEvent sound) {
        buffer.writeIdentifier(sound.location());
    }

    /** Reads a sound written by {@link #writeSound}; null if this game does not know it. */
    @Nullable
    public static SoundEvent readSound(FriendlyByteBuf buffer) {
        return BuiltInRegistries.SOUND_EVENT.getValue(buffer.readIdentifier());
    }

    /**
     * Gives the entity an id followed by {@code segments} free ids for its hull segments (like part entities): the
     * server reserves them (its id counter counts up, so they follow the entity's id); a client gets the entity's id
     * from the server.
     */
    public static void reserveSegmentIds(Entity entity, int segments) {
        if (entity.level().isClientSide()) return;
        for (int i = 0; i < segments; i++) entity.level().getNextEntityId();
        entity.setId(entity.getId());
    }

    /** The dimension's id, e.g. "minecraft:overworld" (the same on client and server). */
    public static String dimensionId(Level level) {
        return level.dimension().identifier().toString();
    }
}
