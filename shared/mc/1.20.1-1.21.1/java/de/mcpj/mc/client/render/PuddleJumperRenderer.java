package de.mcpj.mc.client.render;

import de.mcpj.mc.Mcpj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.mcpj.core.jumper.Jumper;
import de.mcpj.core.jumper.JumperAnimation;
import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.core.render.EnergyBubble;
import de.mcpj.core.render.IonPlume;
import de.mcpj.core.render.JumperLook;
import de.mcpj.core.render.ObjMesh;
import de.mcpj.core.render.VertexSink;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Draws the jumper with the core's mesh, attitude and effects (render types and pose are Minecraft's). */
public class PuddleJumperRenderer<T extends PuddleJumperEntity> extends EntityRenderer<T> {
    static final ResourceLocation TEXTURE = Mcpj.id("textures/entity/puddle_jumper.png");
    // atlas region of the ion plume in the ship texture (pixels 768..1024 x 1536..2048 of 1024 x 2048)
    private static final IonPlume.Uv PLUME_UV = new IonPlume.Uv(0.751f, 0.751f, 0.999f, 0.999f);

    private record ShipStyle(RenderType solid, float brightness, int lightmap, boolean visible, boolean effects) {}

    public PuddleJumperRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = JumperLook.SHADOW_RADIUS;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }

    @Override
    public void render(T jumper, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        ObjMesh model = ModelCache.jumper();
        if (model == null || jumper.construction().isHidden()) return;
        Minecraft minecraft = Minecraft.getInstance();
        Jumper core = jumper.core();
        JumperAnimation animation = core.animation();
        boolean ownShip = minecraft.player != null && jumper.hasPassenger(minecraft.player);
        boolean cloaked = JumperLook.isCloaked(core, partialTick);
        float construction = jumper.constructionProgress();
        ShipStyle style = style(cloaked, ownShip, construction, light);
        shadowRadius = cloaked || construction >= 0 ? 0 : JumperLook.SHADOW_RADIUS;
        float time = jumper.tickCount + partialTick;

        poseStack.pushPose();
        applyAttitude(JumperLook.attitude(core, partialTick), poseStack);

        poseStack.pushPose();
        poseStack.scale(JumperGeometry.MODEL_SCALE, JumperGeometry.MODEL_SCALE, JumperGeometry.MODEL_SCALE);
        boolean pilotView = ownShip && minecraft.options.getCameraType().isFirstPerson() && minecraft.getCameraEntity() == minecraft.player;
        if (style.visible && !pilotView) renderPart(model, "hull", poseStack, buffers, style);
        renderPods(animation, model, poseStack, buffers, style, partialTick, time);
        poseStack.popPose();

        float intensity = EnergyBubble.intensity(animation.shield(partialTick), animation.cloak(partialTick),
                animation.shieldFlash(partialTick), ownShip);
        if (EnergyBubble.isVisible(intensity)) {
            DeferredBubbles.add(poseStack, intensity, EnergyBubble.wobble(animation.shield(partialTick)), time);
        }
        poseStack.popPose();
        super.render(jumper, entityYaw, partialTick, poseStack, buffers, light);
    }

    /** {@code construction}: build progress of a jumper under construction (shown like the own cloaked ship), else negative. */
    private static ShipStyle style(boolean cloaked, boolean ownShip, float construction, int light) {
        if (construction >= 0) {
            return new ShipStyle(RenderType.eyes(TEXTURE), JumperLook.constructionBrightness(construction), LightTexture.FULL_BRIGHT, true, false);
        }
        boolean ghost = cloaked && ownShip;
        if (ghost) return new ShipStyle(RenderType.eyes(TEXTURE), JumperLook.GHOST_BRIGHTNESS, LightTexture.FULL_BRIGHT, true, false);
        return new ShipStyle(RenderType.entityCutout(TEXTURE), 1f, light, !cloaked, !cloaked);
    }

    private static void applyAttitude(JumperLook.Attitude attitude, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-attitude.yaw()));
        poseStack.translate(0, attitude.pivotHeight() + attitude.bob(), 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(attitude.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(attitude.roll()));
        poseStack.translate(0, -attitude.pivotHeight(), 0);
    }

    private static void renderPods(JumperAnimation animation, ObjMesh model, PoseStack poseStack, MultiBufferSource buffers,
                                   ShipStyle style, float partialTick, float time) {
        float extension = animation.podExtension(partialTick);
        float glow = animation.engineGlow(partialTick);
        for (int side = -1; side <= 1; side += 2) {
            String pod = side > 0 ? "pod_left" : "pod_right";
            poseStack.pushPose();
            poseStack.translate(JumperLook.podOffsetX(side, extension), JumperLook.podOffsetY(extension), 0);
            if (style.visible) renderPart(model, pod, poseStack, buffers, style);
            if (style.effects && JumperLook.showsEngineGlow(glow)) {
                renderGlow(model, pod + "_glow", poseStack, buffers, glow);
                IonPlume.emitPod(glowSink(poseStack, buffers), PLUME_UV, side, glow, (float) animation.speed(), time);
            }
            if (style.effects && JumperLook.showsDronePorts(extension)) renderGlow(model, pod + "_ports", poseStack, buffers, extension);
            poseStack.popPose();
        }
    }

    private static void renderPart(ObjMesh model, String group, PoseStack poseStack, MultiBufferSource buffers, ShipStyle style) {
        model.emit(group, RenderSinks.sink(poseStack.last(), buffers.getBuffer(style.solid), style.lightmap, OverlayTexture.NO_OVERLAY),
                style.brightness, style.brightness, style.brightness, 1f);
    }

    private static void renderGlow(ObjMesh model, String group, PoseStack poseStack, MultiBufferSource buffers, float brightness) {
        model.emit(group, glowSink(poseStack, buffers), brightness, brightness, brightness, 1f);
    }

    private static VertexSink glowSink(PoseStack poseStack, MultiBufferSource buffers) {
        return RenderSinks.sink(poseStack.last(), buffers.getBuffer(RenderType.eyes(TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
