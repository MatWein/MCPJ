package de.matthias.puddlejumper.client.sound;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.client.PilotContext;
import de.matthias.puddlejumper.config.ClientConfig;
import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PuddleJumperMod.MOD_ID, value = Dist.CLIENT)
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
