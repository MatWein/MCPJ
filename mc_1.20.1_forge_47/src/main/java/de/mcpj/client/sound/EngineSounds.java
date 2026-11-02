package de.mcpj.client.sound;

import de.mcpj.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public final class EngineSounds {
    private static final Map<Integer, JumperEngineSound> PLAYING = new HashMap<>();

    private EngineSounds() {}

    public static void tick(PuddleJumperEntity jumper) {
        JumperEngineSound sound = PLAYING.get(jumper.getId());
        if (jumper.enginesRunning() && (sound == null || sound.isStopped() || !sound.belongsTo(jumper))) {
            sound = new JumperEngineSound(jumper);
            PLAYING.put(jumper.getId(), sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
        PLAYING.values().removeIf(JumperEngineSound::isStopped);
    }
}
