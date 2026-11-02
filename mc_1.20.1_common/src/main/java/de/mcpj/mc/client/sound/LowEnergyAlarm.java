package de.mcpj.mc.client.sound;

import de.mcpj.mc.Mcpj;
import de.mcpj.mc.client.PilotContext;
import de.mcpj.core.hud.JumperHudModel;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class LowEnergyAlarm {
    private static final float PITCH = 0.6f;
    private static final float VOLUME = 0.35f;

    private LowEnergyAlarm() {}

    /** End of every client tick. */
    public static void onClientTickEnd() {
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (jumper != null && JumperHudModel.lowEnergyAlarm(jumper.core(), jumper.tickCount)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), PITCH, VOLUME));
        }
    }
}
