package de.matthias.puddlejumper.client;

import de.matthias.puddlejumper.ModRegistry;
import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Schleifender Triebwerkssound, Lautstärke/Tonhöhe folgen der Geschwindigkeit. */
public class JumperEngineSound extends AbstractTickableSoundInstance {
    private final PuddleJumperEntity jumper;

    public JumperEngineSound(PuddleJumperEntity jumper) {
        super(ModRegistry.ENGINE.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.jumper = jumper;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.x = jumper.getX();
        this.y = jumper.getY();
        this.z = jumper.getZ();
    }

    public boolean isFor(PuddleJumperEntity j) {
        return jumper == j;
    }

    @Override
    public void tick() {
        if (jumper.isRemoved() || !jumper.isVehicle()) {
            volume -= 0.05f;
            if (volume <= 0f) stop();
            return;
        }
        x = jumper.getX();
        y = jumper.getY() + 1.0;
        z = jumper.getZ();
        float speed = (float) Math.min(jumper.clientSpeed, 1.3);
        float target = 0.35f + speed * 0.5f;
        if (jumper.isCloaked()) target *= 0.5f;
        volume += (target - volume) * 0.1f;
        pitch = 0.75f + speed * 0.4f;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
