package de.mcpj.fabric.block;

import de.mcpj.core.pad.ChargingPad;
import de.mcpj.fabric.registry.FabricObjects;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The charging pad on Fabric; energy comes in through Team Reborn Energy if installed (see TeamRebornEnergy). */
public class FabricChargingPadBlockEntity extends ChargingPadBlockEntity {
    public FabricChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(FabricObjects.CHARGING_PAD_ENTITY, pos, state);
    }

    public ChargingPad logic() {
        return pad();
    }
}
