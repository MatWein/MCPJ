package de.mcpj.mc.client.sound;

import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class JumperEngineSound extends AbstractTickableSoundInstance {
    private static final float START_VOLUME = 0.01f;
    private static final float FADE_OUT_STEP = 0.05f;
    private static final double SOURCE_HEIGHT = 1.0;
    private static final double MAX_AUDIBLE_SPEED = 1.3;
    private static final float BASE_VOLUME_PILOTED = 0.35f;
    private static final float BASE_VOLUME = 0.25f;
    private static final float VOLUME_PER_SPEED = 0.5f;
    private static final float CLOAKED_VOLUME_FACTOR = 0.5f;
    private static final float VOLUME_RESPONSE = 0.1f;
    private static final float BASE_PITCH = 0.75f;
    private static final float PITCH_PER_SPEED = 0.4f;

    private final PuddleJumperEntity jumper;

    public JumperEngineSound(PuddleJumperEntity jumper) {
        super(McpjPlatform.get().engineSound(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.jumper = jumper;
        looping = true;
        delay = 0;
        volume = START_VOLUME;
        followJumper();
    }

    public boolean belongsTo(PuddleJumperEntity other) {
        return jumper == other;
    }

    @Override
    public void tick() {
        if (jumper.isRemoved() || !jumper.enginesRunning()) {
            volume -= FADE_OUT_STEP;
            if (volume <= 0f) stop();
            return;
        }
        followJumper();
        float speed = (float) Math.min(jumper.animation().speed(), MAX_AUDIBLE_SPEED);
        float target = (jumper.isVehicle() ? BASE_VOLUME_PILOTED : BASE_VOLUME) + speed * VOLUME_PER_SPEED;
        if (jumper.isCloaked()) target *= CLOAKED_VOLUME_FACTOR;
        volume += (target - volume) * VOLUME_RESPONSE;
        pitch = BASE_PITCH + speed * PITCH_PER_SPEED;
    }

    private void followJumper() {
        x = jumper.getX();
        y = jumper.getY() + SOURCE_HEIGHT;
        z = jumper.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
