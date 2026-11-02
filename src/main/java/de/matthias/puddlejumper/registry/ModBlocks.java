package de.matthias.puddlejumper.registry;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.block.ChargingPadBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PuddleJumperMod.MOD_ID);

    private static final float PAD_HARDNESS = 3.5f;
    private static final float PAD_BLAST_RESISTANCE = 6.0f;

    public static final RegistryObject<Block> CHARGING_PAD = BLOCKS.register("charging_pad",
            () -> new ChargingPadBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(PAD_HARDNESS, PAD_BLAST_RESISTANCE)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(ChargingPadBlock::lightLevel)));

    private ModBlocks() {}
}
