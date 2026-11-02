package de.mcpj.forge.gametest;

import de.mcpj.mc.gametest.GameTestHooks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.function.Function;

/** The shared game tests on Forge: teleporters, the travel event and Forge Energy. */
public final class ForgeGameTestHooks implements GameTestHooks {
    @Override
    public Entity changeDimension(Entity entity, ServerLevel destination, Vec3 position, float yaw) {
        return entity.changeDimension(destination, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity traveller, ServerLevel level, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(position, Vec3.ZERO, yaw, 0f);
            }

            @Override
            public boolean isVanilla() {
                return false;
            }
        });
    }

    @Override
    public boolean mayTravelAlone(Entity entity, ResourceKey<Level> destination) {
        return ForgeHooks.onTravelToDimension(entity, destination);
    }

    @Override
    public int fillChargingPad(BlockEntity pad, int maxPerInsert) {
        IEnergyStorage storage = pad.getCapability(ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        while (storage.receiveEnergy(maxPerInsert, false) > 0) {
            // keep inserting until the buffer is full
        }
        return storage.getEnergyStored();
    }
}
