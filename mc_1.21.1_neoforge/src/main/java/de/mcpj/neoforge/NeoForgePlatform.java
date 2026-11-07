package de.mcpj.neoforge;

import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import de.mcpj.mc.client.PilotContext;
import de.mcpj.mc.block.JumperProjectorBlockEntity;
import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.mc.client.input.KeyBindings;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.neoforge.block.NeoForgeChargingPadBlockEntity;
import de.mcpj.neoforge.registry.NeoForgeObjects;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

/** The shared Minecraft 26.3 code on NeoForge. */
final class NeoForgePlatform implements McpjPlatform {
    /** Active only while piloting, so keys like Tab, Q, E, I and L keep their vanilla meaning elsewhere. */
    private static final IKeyConflictContext WHILE_PILOTING = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return PilotContext.isPiloting();
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this;
        }
    };

    @Override
    public EntityType<? extends PuddleJumperEntity> jumperType() {
        return NeoForgeObjects.PUDDLE_JUMPER.get();
    }

    @Override
    public EntityType<? extends DroneEntity> droneType() {
        return NeoForgeObjects.DRONE.get();
    }

    @Override
    public Item jumperItem() {
        return NeoForgeObjects.PUDDLE_JUMPER_ITEM.get();
    }

    @Override
    public BlockEntityType<JumperProjectorBlockEntity> projectorType() {
        return NeoForgeObjects.JUMPER_PROJECTOR_ENTITY.get();
    }

    @Override
    public BlockEntityType<? extends ChargingPadBlockEntity> chargingPadType() {
        return NeoForgeObjects.CHARGING_PAD_ENTITY.get();
    }

    @Override
    public ChargingPadBlockEntity newChargingPad(BlockPos pos, BlockState state) {
        return new NeoForgeChargingPadBlockEntity(pos, state);
    }

    @Override
    public SoundEvent engineSound() {
        return NeoForgeObjects.ENGINE.get();
    }

    @Override
    public void sendToServer(Object message) {
        PacketDistributor.sendToServer((CustomPacketPayload) message);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        PacketDistributor.sendToPlayer(player, (CustomPacketPayload) message);
    }

    @Override
    public boolean allowProjectileImpact(Projectile projectile, HitResult hit) {
        return !EventHooks.onProjectileImpact(projectile, hit);
    }

    @Override
    public KeyMapping keyMapping(String name, int defaultKey, boolean onlyWhilePiloting) {
        return new KeyMapping("key.mcpj." + name, onlyWhilePiloting ? WHILE_PILOTING : KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, defaultKey, KeyBindings.CATEGORY);
    }

    @Override
    public InputConstants.Key boundKey(KeyMapping key) {
        return key.getKey();
    }
}
