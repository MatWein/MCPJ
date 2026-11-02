package de.mcpj.forge.block;

import de.mcpj.forge.registry.ModBlockEntities;
import de.mcpj.mc.block.ChargingPadBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/** The charging pad on Forge: Forge Energy goes in from all sides (Mekanism, Thermal, ...). */
public class ForgeChargingPadBlockEntity extends ChargingPadBlockEntity {
    private final IEnergyStorage storage = new PadEnergyStorage(pad());
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> storage);

    public ForgeChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_PAD.get(), pos, state);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) return energyCapability.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCapability = LazyOptional.of(() -> storage);
    }
}
