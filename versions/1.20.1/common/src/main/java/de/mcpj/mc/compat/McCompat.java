package de.mcpj.mc.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import de.mcpj.mc.Mcpj;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
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
import org.joml.Vector3f;

/**
 * Small Minecraft API differences, so that shared code (shared/mc/all) compiles against every version. Each
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

    /** A new jumper at that spot and heading (not yet in the world); null if it could not be created. */
    @Nullable
    public static PuddleJumperEntity newJumper(Level level, double x, double y, double z, float yaw) {
        PuddleJumperEntity jumper = McpjPlatform.get().jumperType().create(level);
        if (jumper != null) jumper.moveTo(x, y, z, yaw, 0f);
        return jumper;
    }

    /** The result of a successful use or interaction (Minecraft 1.20.1/1.21.1 tell client and server apart). */
    public static InteractionResult success(Level level) {
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Text stored on the item, empty if there is none. */
    public static String itemText(ItemStack stack, String key) {
        return stack.hasTag() ? stack.getTag().getString(key) : "";
    }

    public static void setItemText(ItemStack stack, String key, String value) {
        stack.getOrCreateTag().putString(key, value);
    }

    /** A message above the hotbar. */
    public static void overlayMessage(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    /** Sends the packet to every player who sees the entity. */
    public static void sendToTracking(ServerLevel level, Entity entity, Packet<? super ClientGamePacketListener> packet) {
        level.getChunkSource().broadcast(entity, packet);
    }

    // ------------------------------------------------------------------ building blocks, items and types (McpjObjects)

    /** Properties of an MCPJ block (newer Minecraft versions want the block's id up front). */
    public static BlockBehaviour.Properties blockProperties(String name) {
        return BlockBehaviour.Properties.of();
    }

    public static Item.Properties itemProperties(String name) {
        return new Item.Properties();
    }

    /** Properties of a block's item (named like the block). */
    public static Item.Properties blockItemProperties(String name) {
        return new Item.Properties();
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        return BlockEntityType.Builder.of(factory, block).build(null);
    }

    public static <T extends Entity> EntityType<T> entityType(EntityType.Builder<T> builder, String name) {
        return builder.build(Mcpj.id(name).toString());
    }

    /** Minecraft 1.20.1 has no eye height in the builder: the jumper sets it itself (JumperEntityCompat). */
    public static <T extends Entity> EntityType.Builder<T> eyeHeight(EntityType.Builder<T> builder, float height) {
        return builder;
    }

    /** Writes the sound's id (for network messages). */
    public static void writeSound(FriendlyByteBuf buffer, SoundEvent sound) {
        buffer.writeResourceLocation(sound.getLocation());
    }

    /** Reads a sound written by {@link #writeSound}; null if this game does not know it. */
    @Nullable
    public static SoundEvent readSound(FriendlyByteBuf buffer) {
        return BuiltInRegistries.SOUND_EVENT.get(buffer.readResourceLocation());
    }

    /** Gives the entity an id followed by {@code segments} free ids for its hull segments (like part entities). */
    public static void reserveSegmentIds(Entity entity, int segments) {
        entity.setId(Entity.ENTITY_COUNTER.getAndAdd(segments + 1) + 1);
    }

    /** The dimension's id, e.g. "minecraft:overworld" (the same on client and server). */
    public static String dimensionId(Level level) {
        return level.dimension().location().toString();
    }
}
