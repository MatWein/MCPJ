package de.mcpj.core.jumper;

import de.mcpj.core.math.MathUtil;
import de.mcpj.core.math.Vec3d;

/** Client-side animation state: pods, shield and cloak fades, engine glow, banking and pitch. */
public final class JumperAnimation {
    private static final float POD_SPEED = 0.035f;
    private static final float SHIELD_FADE_SPEED = 0.06f;
    private static final float CLOAK_FADE_SPEED = 0.05f;
    private static final int SHIELD_FLASH_TICKS = 10;
    private static final float GLOW_IDLE_PILOTED = 0.25f;
    private static final float GLOW_AIRBORNE = 0.6f;
    private static final float GLOW_PER_SPEED = 0.5f;
    private static final float GLOW_SPEED_BONUS_MAX = 0.4f;
    private static final float GLOW_RESPONSE = 0.08f;
    private static final float ROLL_PER_TURN = 5f;
    private static final float ROLL_PER_SIDE_SPEED = 28f;
    private static final float MAX_ROLL = 30f;
    private static final float PITCH_PER_CLIMB = 28f;
    private static final float PITCH_PER_SPEED = 5f;
    private static final float MAX_PITCH = 28f;
    private static final float TILT_RESPONSE = 0.12f;

    private final Jumper jumper;
    private final JumperExhaust exhaust;
    private float podExtension, podExtensionOld;
    private float shield, shieldOld;
    private float cloak, cloakOld;
    private float engineGlow, engineGlowOld;
    private float roll, rollOld;
    private float pitch, pitchOld;
    private int shieldFlash;
    private double speed;

    JumperAnimation(Jumper jumper) {
        this.jumper = jumper;
        this.exhaust = new JumperExhaust(jumper);
    }

    void tick() {
        JumperHost host = jumper.host();
        Vec3d velocity = host.location().subtract(host.previousLocation());
        speed = velocity.length();
        podExtensionOld = podExtension;
        podExtension = MathUtil.approach(podExtension, jumper.isPodsOut() ? 1f : 0f, POD_SPEED);
        shieldOld = shield;
        shield = MathUtil.approach(shield, jumper.isShielded() ? 1f : 0f, SHIELD_FADE_SPEED);
        cloakOld = cloak;
        cloak = MathUtil.approach(cloak, jumper.isCloaked() ? 1f : 0f, CLOAK_FADE_SPEED);
        if (shieldFlash > 0) shieldFlash--;
        engineGlowOld = engineGlow;
        engineGlow += (targetGlow() - engineGlow) * GLOW_RESPONSE;
        updateTilt(velocity);
        exhaust.spawn(engineGlow, podExtension(1f), velocity);
    }

    private float targetGlow() {
        if (!jumper.energy().hasEnergy()) return 0f;
        if (jumper.host().onGround()) return jumper.host().hasPassengers() ? GLOW_IDLE_PILOTED : 0f;
        return GLOW_AIRBORNE + MathUtil.clamp((float) speed * GLOW_PER_SPEED, 0f, GLOW_SPEED_BONUS_MAX);
    }

    private void updateTilt(Vec3d velocity) {
        Vec3d left = jumper.left();
        Vec3d forward = jumper.forward();
        double sideSpeed = left.x() * velocity.x() + left.z() * velocity.z();
        double forwardSpeed = forward.x() * velocity.x() + forward.z() * velocity.z();
        float yawDelta = MathUtil.wrapDegrees(jumper.host().yaw() - jumper.host().previousYaw());
        float targetRoll = MathUtil.clamp(yawDelta * ROLL_PER_TURN - (float) sideSpeed * ROLL_PER_SIDE_SPEED, -MAX_ROLL, MAX_ROLL);
        float targetPitch = MathUtil.clamp((float) (-velocity.y() * PITCH_PER_CLIMB + forwardSpeed * PITCH_PER_SPEED), -MAX_PITCH, MAX_PITCH);
        rollOld = roll;
        pitchOld = pitch;
        roll += (targetRoll - roll) * TILT_RESPONSE;
        pitch += (targetPitch - pitch) * TILT_RESPONSE;
    }

    /** The shield absorbed a hit (event from the server). */
    public void flashShield() {
        shieldFlash = SHIELD_FLASH_TICKS;
    }

    public double speed() {
        return speed;
    }

    public float podExtension(float partialTick) {
        float t = MathUtil.lerp(partialTick, podExtensionOld, podExtension);
        return t * t * (3 - 2 * t);
    }

    public float shield(float partialTick) {
        return MathUtil.lerp(partialTick, shieldOld, shield);
    }

    public float cloak(float partialTick) {
        return MathUtil.lerp(partialTick, cloakOld, cloak);
    }

    public float engineGlow(float partialTick) {
        return MathUtil.lerp(partialTick, engineGlowOld, engineGlow);
    }

    public float roll(float partialTick) {
        return MathUtil.lerp(partialTick, rollOld, roll);
    }

    public float pitch(float partialTick) {
        return MathUtil.lerp(partialTick, pitchOld, pitch);
    }

    public float shieldFlash(float partialTick) {
        return Math.max(0, shieldFlash - partialTick) / SHIELD_FLASH_TICKS;
    }

    public boolean podsMostlyOut() {
        return podExtension > 0.5f;
    }
}
