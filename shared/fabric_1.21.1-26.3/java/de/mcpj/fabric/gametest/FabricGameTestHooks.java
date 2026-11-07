package de.mcpj.fabric.gametest;

import de.mcpj.core.pad.ChargingPad;
import de.mcpj.fabric.block.FabricChargingPadBlockEntity;
import de.mcpj.mc.gametest.GameTestHooks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The shared game tests on Fabric: vanilla's travel rule; pads are filled directly (no energy API in the test run). */
public final class FabricGameTestHooks implements GameTestHooks {
    @Override
    public boolean mayTravelAlone(Entity entity, ResourceKey<Level> destination) {
        return entity.canUsePortal(false);
    }

    @Override
    public int fillChargingPad(BlockEntity pad, int maxPerInsert) {
        ChargingPad logic = ((FabricChargingPadBlockEntity) pad).logic();
        while (logic.receive(maxPerInsert, false) > 0) {
            // keep inserting until the buffer is full
        }
        return logic.stored();
    }
}
