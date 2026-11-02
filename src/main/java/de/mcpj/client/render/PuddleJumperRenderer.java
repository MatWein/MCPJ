package de.mcpj.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.mcpj.McpjMod;
import de.mcpj.entity.jumper.JumperAnimation;
import de.mcpj.entity.jumper.JumperGeometry;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PuddleJumperRenderer extends EntityRenderer<PuddleJumperEntity> {
    static final ResourceLocation TEXTURE = McpjMod.id("textures/entity/puddle_jumper.png");
    private static final float SHADOW_RADIUS = 2.2f * JumperGeometry.RENDER_SCALE;
    private static final float CLOAKED_THRESHOLD = 0.5f;
    private static final float GHOST_BRIGHTNESS = 0.22f;
    private static final float HOVER_BOB_SPEED = 0.08f;
    private static final float HOVER_BOB_AMPLITUDE = 0.04f;
    private static final float HURT_WOBBLE_DIVISOR = 400f;
    private static final float MIN_VISIBLE_GLOW = 0.02f;
    private static final float MIN_PORT_GLOW = 0.05f;
    private static final float SHIELD_BASE_INTENSITY = 0.55f;
    private static final float SHIELD_FLASH_INTENSITY = 0.6f;
    private static final float CLOAK_INTENSITY_OWN = 0.2f;
    private static final float CLOAK_INTENSITY_OTHERS = 0.09f;
    private static final float SHIELD_WOBBLE = 0.02f;
    private static final float MIN_BUBBLE_INTENSITY = 0.005f;
    private static final float PLUME_PHASE_PER_SIDE = 7f;
    private static final float PLUME_PHASE_PER_NOZZLE = 13f;
    private static final float PLUME_START_BEHIND_NOZZLE = 0.12f;

    private record ShipStyle(RenderType solid, RenderType glow, float brightness, int lightmap, boolean visible, boolean effects) {}

    public PuddleJumperRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = SHADOW_RADIUS;
    }

    @Override
    public ResourceLocation getTextureLocation(PuddleJumperEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PuddleJumperEntity jumper, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        ObjModel model = ModelCache.jumper();
        if (model == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        JumperAnimation animation = jumper.animation();
        boolean ownShip = minecraft.player != null && jumper.hasPassenger(minecraft.player);
        boolean cloaked = animation.cloak(partialTick) > CLOAKED_THRESHOLD;
        ShipStyle style = style(cloaked, ownShip, light);
        shadowRadius = cloaked ? 0 : SHADOW_RADIUS;
        float time = jumper.tickCount + partialTick;

        poseStack.pushPose();
        applyAttitude(jumper, partialTick, poseStack);

        poseStack.pushPose();
        poseStack.scale(JumperGeometry.MODEL_SCALE, JumperGeometry.MODEL_SCALE, JumperGeometry.MODEL_SCALE);
        boolean pilotView = ownShip && minecraft.options.getCameraType().isFirstPerson() && minecraft.getCameraEntity() == minecraft.player;
        if (style.visible && !pilotView) renderPart(model, "hull", poseStack, buffers, style);
        renderPods(jumper, model, poseStack, buffers, style, partialTick, time);
        poseStack.popPose();

        renderBubble(animation, ownShip, partialTick, time, poseStack, buffers);
        poseStack.popPose();
        super.render(jumper, entityYaw, partialTick, poseStack, buffers, light);
    }

    private static ShipStyle style(boolean cloaked, boolean ownShip, int light) {
        RenderType glow = RenderType.eyes(TEXTURE);
        boolean ghost = cloaked && ownShip;
        if (ghost) return new ShipStyle(glow, glow, GHOST_BRIGHTNESS, LightTexture.FULL_BRIGHT, true, false);
        return new ShipStyle(RenderType.entityCutout(TEXTURE), glow, 1f, light, !cloaked, !cloaked);
    }

    private static void applyAttitude(PuddleJumperEntity jumper, float partialTick, PoseStack poseStack) {
        JumperAnimation animation = jumper.animation();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, jumper.yRotO, jumper.getYRot())));
        float roll = animation.roll(partialTick);
        float hurt = jumper.hurtTime() - partialTick;
        if (hurt > 0) {
            float wobble = Math.max(0, jumper.hurtWobble() - partialTick);
            roll += Mth.sin(hurt) * hurt * wobble / HURT_WOBBLE_DIVISOR * jumper.hurtDirection();
        }
        float bob = jumper.onGround() ? 0f : Mth.sin((jumper.tickCount + partialTick) * HOVER_BOB_SPEED) * HOVER_BOB_AMPLITUDE;
        poseStack.translate(0, JumperGeometry.TILT_PIVOT_HEIGHT + bob, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(animation.pitch(partialTick)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0, -JumperGeometry.TILT_PIVOT_HEIGHT, 0);
    }

    private static void renderPods(PuddleJumperEntity jumper, ObjModel model, PoseStack poseStack, MultiBufferSource buffers,
                                   ShipStyle style, float partialTick, float time) {
        float extension = jumper.animation().podExtension(partialTick);
        float glow = jumper.animation().engineGlow(partialTick);
        for (int side = -1; side <= 1; side += 2) {
            String pod = side > 0 ? "pod_left" : "pod_right";
            poseStack.pushPose();
            poseStack.translate(side * JumperGeometry.POD_EXTENSION * extension, -JumperGeometry.POD_DROP * extension, 0);
            if (style.visible) renderPart(model, pod, poseStack, buffers, style);
            if (style.effects && glow > MIN_VISIBLE_GLOW) {
                renderGlow(model, pod + "_glow", poseStack, buffers, glow);
                renderPlumes(poseStack, buffers, side, glow, (float) jumper.animation().speed(), time);
            }
            if (style.effects && extension > MIN_PORT_GLOW) renderGlow(model, pod + "_ports", poseStack, buffers, extension);
            poseStack.popPose();
        }
    }

    private static void renderPlumes(PoseStack poseStack, MultiBufferSource buffers, int side, float glow, float speed, float time) {
        for (float nozzle : new float[]{-JumperGeometry.NOZZLE_OFFSET_X, JumperGeometry.NOZZLE_OFFSET_X}) {
            IonPlumeRenderer.render(poseStack, buffers.getBuffer(RenderType.eyes(TEXTURE)),
                    side * JumperGeometry.POD_X + nozzle, JumperGeometry.POD_Y, JumperGeometry.POD_REAR_Z - PLUME_START_BEHIND_NOZZLE,
                    glow, speed, time + side * PLUME_PHASE_PER_SIDE + nozzle * PLUME_PHASE_PER_NOZZLE);
        }
    }

    private static void renderPart(ObjModel model, String group, PoseStack poseStack, MultiBufferSource buffers, ShipStyle style) {
        model.render(group, poseStack.last(), buffers.getBuffer(style.solid), style.lightmap, OverlayTexture.NO_OVERLAY,
                style.brightness, style.brightness, style.brightness, 1f);
    }

    private static void renderGlow(ObjModel model, String group, PoseStack poseStack, MultiBufferSource buffers, float brightness) {
        model.render(group, poseStack.last(), buffers.getBuffer(RenderType.eyes(TEXTURE)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, brightness, brightness, brightness, 1f);
    }

    private static void renderBubble(JumperAnimation animation, boolean ownShip, float partialTick, float time,
                                     PoseStack poseStack, MultiBufferSource buffers) {
        float shield = animation.shield(partialTick);
        float cloak = animation.cloak(partialTick);
        float shieldIntensity = shield * (SHIELD_BASE_INTENSITY + SHIELD_FLASH_INTENSITY * animation.shieldFlash(partialTick));
        float cloakIntensity = cloak * (1 - shield) * (ownShip ? CLOAK_INTENSITY_OWN : CLOAK_INTENSITY_OTHERS);
        float intensity = shieldIntensity + cloakIntensity;
        if (intensity > MIN_BUBBLE_INTENSITY) DeferredBubbles.add(poseStack, intensity, SHIELD_WOBBLE * shield, time);
    }
}
