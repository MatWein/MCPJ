package de.mcpj.mc.platform;

import de.mcpj.mc.block.JumperProjectorBlockEntity;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * What the shared Minecraft code needs from the mod loader: its registered objects, the network transport and
 * a few hooks. Each loader module sets its implementation once while the mod initializes ({@link #set}).
 */
public interface McpjPlatform {
    EntityType<? extends PuddleJumperEntity> jumperType();

    EntityType<? extends DroneEntity> droneType();

    Item jumperItem();

    Item jumperKeyItem();

    BlockEntityType<? extends ChargingPadBlockEntity> chargingPadType();

    /** The loader's charging pad block entity (it adds the loader's energy API). */
    ChargingPadBlockEntity newChargingPad(BlockPos pos, BlockState state);

    BlockEntityType<JumperProjectorBlockEntity> projectorType();

    SoundEvent engineSound();

    /** Client: sends a message (see de.mcpj.mc.network) to the server. */
    void sendToServer(Object message);

    void sendToPlayer(ServerPlayer player, Object message);

    /** Lets other mods veto a projectile impact (Forge has an event for it). */
    boolean allowProjectileImpact(Projectile projectile, HitResult hit);

    /**
     * A key binding of the mod; {@code onlyWhilePiloting} keys may share a key with vanilla (e.g. Q, E, Tab),
     * which the loader allows in its own way.
     */
    KeyMapping keyMapping(String name, int defaultKey, boolean onlyWhilePiloting);

    /** The key a key binding is currently bound to (vanilla keeps it private). */
    InputConstants.Key boundKey(KeyMapping key);

    static McpjPlatform get() {
        McpjPlatform platform = Holder.instance;
        if (platform == null) throw new IllegalStateException("MCPJ platform not initialized");
        return platform;
    }

    static void set(McpjPlatform platform) {
        Holder.instance = platform;
    }

    final class Holder {
        private static McpjPlatform instance;

        private Holder() {}
    }
}
