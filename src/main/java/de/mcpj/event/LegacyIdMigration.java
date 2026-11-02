package de.mcpj.event;

import com.mojang.logging.LogUtils;
import de.mcpj.McpjMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import org.slf4j.Logger;

/**
 * The mod id used to be {@code puddlejumper}. Worlds from that time still contain jumpers, charging pads, items and
 * block entities under the old namespace; they are mapped to the same names under {@code mcpj} so nothing is lost.
 */
@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class LegacyIdMigration {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String LEGACY_NAMESPACE = "puddlejumper";

    private LegacyIdMigration() {}

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        remap(event, ForgeRegistries.BLOCKS);
        remap(event, ForgeRegistries.ITEMS);
        remap(event, ForgeRegistries.ENTITY_TYPES);
        remap(event, ForgeRegistries.BLOCK_ENTITY_TYPES);
        remap(event, ForgeRegistries.SOUND_EVENTS);
    }

    private static <T> void remap(MissingMappingsEvent event, IForgeRegistry<T> registry) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(registry.getRegistryKey(), LEGACY_NAMESPACE)) {
            ResourceLocation replacement = McpjMod.id(mapping.getKey().getPath());
            // containsKey first: defaulted registries (blocks, items) return air for unknown names
            if (registry.containsKey(replacement)) {
                mapping.remap(registry.getValue(replacement));
                LOGGER.info("Migrated {} to {}", mapping.getKey(), replacement);
            }
        }
    }
}
