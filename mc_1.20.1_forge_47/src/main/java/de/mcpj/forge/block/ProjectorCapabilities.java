package de.mcpj.forge.block;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.Mcpj;
import de.mcpj.mc.block.JumperProjectorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The jumper projector takes Forge Energy from all sides (the shared block entity has no Forge subclass). */
@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class ProjectorCapabilities {
    private ProjectorCapabilities() {}

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof JumperProjectorBlockEntity projector)) return;
        LazyOptional<InputEnergyStorage> energy = LazyOptional.of(() -> new InputEnergyStorage(projector.logic()));
        event.addCapability(Mcpj.id("energy"), new ICapabilityProvider() {
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
                return capability == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
            }
        });
        event.addListener(energy::invalidate);
    }
}
