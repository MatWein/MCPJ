package de.mcpj.forge;

import de.mcpj.mc.block.JumperProjectorBlockEntity;
import com.mojang.blaze3d.platform.InputConstants;
import de.mcpj.forge.block.ForgeChargingPadBlockEntity;
import de.mcpj.forge.network.ModNetwork;
import de.mcpj.forge.registry.ModBlockEntities;
import de.mcpj.forge.registry.ModEntities;
import de.mcpj.forge.registry.ModItems;
import de.mcpj.forge.registry.ModSounds;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import de.mcpj.mc.client.PilotContext;
import de.mcpj.mc.client.input.KeyBindings;
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
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.ForgeEventFactory;

/** The shared Minecraft 1.20.1 code on Forge. */
final class ForgePlatform implements McpjPlatform {
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
        return ModEntities.PUDDLE_JUMPER.get();
    }

    @Override
    public EntityType<? extends DroneEntity> droneType() {
        return ModEntities.DRONE.get();
    }

    @Override
    public Item jumperItem() {
        return ModItems.PUDDLE_JUMPER.get();
    }

    @Override
    public BlockEntityType<? extends ChargingPadBlockEntity> chargingPadType() {
        return ModBlockEntities.CHARGING_PAD.get();
    }

    @Override
    public BlockEntityType<JumperProjectorBlockEntity> projectorType() {
        return ModBlockEntities.JUMPER_PROJECTOR.get();
    }

    @Override
    public ChargingPadBlockEntity newChargingPad(BlockPos pos, BlockState state) {
        return new ForgeChargingPadBlockEntity(pos, state);
    }

    @Override
    public SoundEvent engineSound() {
        return ModSounds.ENGINE.get();
    }

    @Override
    public void sendToServer(Object message) {
        ModNetwork.sendToServer(message);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        ModNetwork.sendToPlayer(player, message);
    }

    @Override
    public boolean allowProjectileImpact(Projectile projectile, HitResult hit) {
        return !ForgeEventFactory.onProjectileImpact(projectile, hit);
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
