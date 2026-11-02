package de.mcpj.client.sound;

import de.mcpj.McpjMod;
import de.mcpj.client.PilotContext;
import de.mcpj.config.ClientConfig;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID, value = Dist.CLIENT)
public final class LowEnergyAlarm {
    private static final int INTERVAL_TICKS = 40;
    private static final float PITCH = 0.6f;
    private static final float VOLUME = 0.35f;

    private LowEnergyAlarm() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (jumper == null || jumper.tickCount % INTERVAL_TICKS != 0) return;
        if (jumper.energy().fraction() < ClientConfig.lowEnergyWarning()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.get(), PITCH, VOLUME));
        }
    }
}
