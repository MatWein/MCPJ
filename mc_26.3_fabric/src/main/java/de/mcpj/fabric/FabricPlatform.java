package de.mcpj.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.fabric.block.FabricChargingPadBlockEntity;
import de.mcpj.fabric.client.FabricClientServices;
import de.mcpj.fabric.network.FabricNetwork;
import de.mcpj.fabric.registry.FabricObjects;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.mc.entity.drone.DroneEntity;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.platform.McpjPlatform;
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

/** The shared Minecraft 26.3 code on Fabric; client-only parts are in FabricClientServices. */
final class FabricPlatform implements McpjPlatform {
    @Override
    public EntityType<? extends PuddleJumperEntity> jumperType() {
        return FabricObjects.PUDDLE_JUMPER;
    }

    @Override
    public EntityType<? extends DroneEntity> droneType() {
        return FabricObjects.DRONE;
    }

    @Override
    public Item jumperItem() {
        return FabricObjects.PUDDLE_JUMPER_ITEM;
    }

    @Override
    public BlockEntityType<? extends ChargingPadBlockEntity> chargingPadType() {
        return FabricObjects.CHARGING_PAD_ENTITY;
    }

    @Override
    public ChargingPadBlockEntity newChargingPad(BlockPos pos, BlockState state) {
        return new FabricChargingPadBlockEntity(pos, state);
    }

    @Override
    public SoundEvent engineSound() {
        return FabricObjects.ENGINE;
    }

    @Override
    public void sendToServer(Object message) {
        FabricClientServices.sendToServer(message);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        FabricNetwork.sendToPlayer(player, message);
    }

    @Override
    public boolean allowProjectileImpact(Projectile projectile, HitResult hit) {
        return true;
    }

    @Override
    public KeyMapping keyMapping(String name, int defaultKey, boolean onlyWhilePiloting) {
        return FabricClientServices.keyMapping(name, defaultKey, onlyWhilePiloting);
    }

    @Override
    public InputConstants.Key boundKey(KeyMapping key) {
        return FabricClientServices.boundKey(key);
    }
}
