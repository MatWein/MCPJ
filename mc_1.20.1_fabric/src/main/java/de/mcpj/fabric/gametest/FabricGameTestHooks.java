package de.mcpj.fabric.gametest;

import de.mcpj.core.pad.ChargingPad;
import de.mcpj.fabric.block.FabricChargingPadBlockEntity;
import de.mcpj.mc.gametest.GameTestHooks;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;

/** The shared game tests on Fabric: Fabric's dimension API and vanilla's travel rule; pads are filled directly. */
public final class FabricGameTestHooks implements GameTestHooks {
    @Override
    public Entity changeDimension(Entity entity, ServerLevel destination, Vec3 position, float yaw) {
        return FabricDimensions.teleport(entity, destination, new PortalInfo(position, Vec3.ZERO, yaw, 0f));
    }

    @Override
    public boolean mayTravelAlone(Entity entity, ResourceKey<Level> destination) {
        return entity.canChangeDimensions();
    }

    /** Team Reborn Energy is optional and not present in the test run: insert through the pad's own logic. */
    @Override
    public int fillChargingPad(BlockEntity pad, int maxPerInsert) {
        ChargingPad logic = ((FabricChargingPadBlockEntity) pad).logic();
        while (logic.receive(maxPerInsert, false) > 0) {
            // keep inserting until the buffer is full
        }
        return logic.stored();
    }
}
