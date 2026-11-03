package de.mcpj.core.render;

import de.mcpj.core.jumper.Jumper;
import de.mcpj.core.jumper.JumperAnimation;
import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.core.jumper.JumperHost;
import de.mcpj.core.jumper.JumperSync;
import de.mcpj.core.math.MathUtil;

/**
 * How the jumper is drawn: attitude (heading, pitch, banking, hit wobble, hover bob), visibility while cloaked,
 * pod positions and glow thresholds. The platform applies these numbers to its pose and render types.
 */
public final class JumperLook {
    public static final float SHADOW_RADIUS = 2.2f * JumperGeometry.RENDER_SCALE;
    /** Brightness of the faint outline the pilot sees of their own cloaked ship. */
    public static final float GHOST_BRIGHTNESS = 0.22f;
    private static final float CONSTRUCTION_START_BRIGHTNESS = 0.6f;
    private static final float CONSTRUCTION_FINAL_BRIGHTNESS = 1.0f;
    private static final float CLOAKED_THRESHOLD = 0.5f;
    private static final float HOVER_BOB_SPEED = 0.08f;
    private static final float HOVER_BOB_AMPLITUDE = 0.04f;
    private static final float HURT_WOBBLE_DIVISOR = 400f;
    private static final float MIN_VISIBLE_GLOW = 0.02f;
    private static final float MIN_PORT_GLOW = 0.05f;

    /**
     * Pose of the ship for one frame. Apply in this order: rotate about Y by {@code -yaw}, move up by
     * {@code pivotHeight + bob}, rotate about X by {@code pitch}, about Z by {@code roll}, move down by {@code pivotHeight}.
     */
    public record Attitude(float yaw, float pitch, float roll, float bob, float pivotHeight) {}

    private JumperLook() {}

    public static Attitude attitude(Jumper jumper, float partialTick) {
        JumperHost host = jumper.host();
        JumperSync sync = jumper.sync();
        JumperAnimation animation = jumper.animation();
        float yaw = host.previousYaw() + partialTick * MathUtil.wrapDegrees(host.yaw() - host.previousYaw());
        float roll = animation.roll(partialTick);
        float hurt = sync.hurtTime() - partialTick;
        if (hurt > 0) {
            float wobble = Math.max(0, sync.hurtWobble() - partialTick);
            roll += (float) Math.sin(hurt) * hurt * wobble / HURT_WOBBLE_DIVISOR * sync.hurtDirection();
        }
        float bob = host.onGround() ? 0f : (float) Math.sin((host.tickCount() + partialTick) * HOVER_BOB_SPEED) * HOVER_BOB_AMPLITUDE;
        return new Attitude(yaw, animation.pitch(partialTick), roll, bob, JumperGeometry.TILT_PIVOT_HEIGHT);
    }

    /** Cloaked enough to be invisible to others (the pilot sees a ghost outline). */
    /** Brightness of the glowing outline of a jumper under construction: clearly visible, brighter as the build goes on. */
    public static float constructionBrightness(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        return CONSTRUCTION_START_BRIGHTNESS + (CONSTRUCTION_FINAL_BRIGHTNESS - CONSTRUCTION_START_BRIGHTNESS) * clamped;
    }

    public static boolean isCloaked(Jumper jumper, float partialTick) {
        return jumper.animation().cloak(partialTick) > CLOAKED_THRESHOLD;
    }

    /** Sideways offset of a pod (side = 1 left, -1 right) for its extension, in model units. */
    public static float podOffsetX(int side, float extension) {
        return side * JumperGeometry.POD_EXTENSION * extension;
    }

    public static float podOffsetY(float extension) {
        return -JumperGeometry.POD_DROP * extension;
    }

    public static boolean showsEngineGlow(float glow) {
        return glow > MIN_VISIBLE_GLOW;
    }

    public static boolean showsDronePorts(float extension) {
        return extension > MIN_PORT_GLOW;
    }
}
