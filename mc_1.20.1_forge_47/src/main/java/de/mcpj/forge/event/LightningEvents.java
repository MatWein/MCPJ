package de.mcpj.forge.event;

import de.mcpj.forge.McpjMod;
import de.mcpj.mc.entity.jumper.JumperLightning;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Jumpers attract natural lightning during thunderstorms (see JumperLightning). */
@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID)
public final class LightningEvents {
    private LightningEvents() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LightningBolt bolt) JumperLightning.attract(bolt);
    }
}
