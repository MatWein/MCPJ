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
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Draws the jumper with the core's mesh, attitude and effects (render types and pose are Minecraft's). */
public class PuddleJumperRenderer<T extends PuddleJumperEntity> extends EntityRenderer<T, PuddleJumperRenderer.State> {
    static final Identifier TEXTURE = Mcpj.id("textures/entity/puddle_jumper.png");
    private static final Identifier SHIELD_TEXTURE = Mcpj.id("textures/entity/shield.png");
    // atlas region of the ion plume in the ship texture (pixels 768..1024 x 1536..2048 of 1024 x 2048)
    private static final IonPlume.Uv PLUME_UV = new IonPlume.Uv(0.751f, 0.751f, 0.999f, 0.999f);

    /** What the renderer needs of the ship in this frame; the animation is read from the core while extracting. */
    public static final class State extends EntityRenderState {
        @Nullable Jumper core;
        float partialTick;
        boolean ownShip;
        boolean pilotView;
        boolean cloaked;
    }

    private record ShipStyle(RenderType solid, float brightness, int lightmap, boolean visible, boolean effects) {}

    public PuddleJumperRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = JumperLook.SHADOW_RADIUS;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T jumper, State state, float partialTick) {
        super.extractRenderState(jumper, state, partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        state.core = jumper.core();
        state.partialTick = partialTick;
        state.ownShip = minecraft.player != null && jumper.hasPassenger(minecraft.player);
        state.pilotView = state.ownShip && minecraft.options.getCameraType().isFirstPerson() && minecraft.getCameraEntity() == minecraft.player;
        state.cloaked = JumperLook.isCloaked(jumper.core(), partialTick);
    }

    @Override
    protected AABB getBoundingBoxForCulling(T jumper, float partialTick) {
        return jumper.cullingBox();
    }

    @Override
    protected float getShadowRadius(State state) {
        return state.cloaked ? 0 : JumperLook.SHADOW_RADIUS;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ObjMesh model = ModelCache.jumper();
        Jumper core = state.core;
        if (model == null || core == null) return;
        float partialTick = state.partialTick;
        JumperAnimation animation = core.animation();
        ShipStyle style = style(state.cloaked, state.ownShip, state.lightCoords);
        float time = state.ageInTicks;

        poseStack.pushPose();
        applyAttitude(JumperLook.attitude(core, partialTick), poseStack);

        poseStack.pushPose();
        poseStack.scale(JumperGeometry.MODEL_SCALE, JumperGeometry.MODEL_SCALE, JumperGeometry.MODEL_SCALE);
        if (style.visible && !state.pilotView) renderPart(model, "hull", poseStack, collector, style);
        renderPods(animation, model, poseStack, collector, style, partialTick, time);
        poseStack.popPose();

        float intensity = EnergyBubble.intensity(animation.shield(partialTick), animation.cloak(partialTick),
                animation.shieldFlash(partialTick), state.ownShip);
        if (EnergyBubble.isVisible(intensity)) {
            renderBubble(poseStack, collector, intensity, EnergyBubble.wobble(animation.shield(partialTick)), time);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static ShipStyle style(boolean cloaked, boolean ownShip, int light) {
        boolean ghost = cloaked && ownShip;
        if (ghost) return new ShipStyle(ModRenderTypes.glow(TEXTURE), JumperLook.GHOST_BRIGHTNESS, LightCoordsUtil.FULL_BRIGHT, true, false);
        return new ShipStyle(RenderTypes.entityCutout(TEXTURE), 1f, light, !cloaked, !cloaked);
    }

    private static void applyAttitude(JumperLook.Attitude attitude, PoseStack poseStack) {
        poseStack.rotateDegrees(Axis.YP, -attitude.yaw());
        poseStack.translate(0, attitude.pivotHeight() + attitude.bob(), 0);
        poseStack.rotateDegrees(Axis.XP, attitude.pitch());
        poseStack.rotateDegrees(Axis.ZP, attitude.roll());
        poseStack.translate(0, -attitude.pivotHeight(), 0);
    }

    private static void renderPods(JumperAnimation animation, ObjMesh model, PoseStack poseStack, SubmitNodeCollector collector,
                                   ShipStyle style, float partialTick, float time) {
        float extension = animation.podExtension(partialTick);
        float glow = animation.engineGlow(partialTick);
        float speed = (float) animation.speed();
        for (int side = -1; side <= 1; side += 2) {
            String pod = side > 0 ? "pod_left" : "pod_right";
            int podSide = side;
            poseStack.pushPose();
            poseStack.translate(JumperLook.podOffsetX(side, extension), JumperLook.podOffsetY(extension), 0);
            if (style.visible) renderPart(model, pod, poseStack, collector, style);
            if (style.effects && JumperLook.showsEngineGlow(glow)) {
                renderGlow(model, pod + "_glow", poseStack, collector, glow);
                collector.submitCustomGeometry(poseStack, ModRenderTypes.glow(TEXTURE), (pose, buffer) ->
                        IonPlume.emitPod(RenderSinks.sink(pose, buffer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY),
                                PLUME_UV, podSide, glow, speed, time));
            }
            if (style.effects && JumperLook.showsDronePorts(extension)) renderGlow(model, pod + "_ports", poseStack, collector, extension);
            poseStack.popPose();
        }
    }

    private static void renderPart(ObjMesh model, String group, PoseStack poseStack, SubmitNodeCollector collector, ShipStyle style) {
        collector.submitCustomGeometry(poseStack, style.solid, (pose, buffer) ->
                model.emit(group, RenderSinks.sink(pose, buffer, style.lightmap, OverlayTexture.NO_OVERLAY),
                        style.brightness, style.brightness, style.brightness, 1f));
    }

    private static void renderGlow(ObjMesh model, String group, PoseStack poseStack, SubmitNodeCollector collector, float brightness) {
        collector.submitCustomGeometry(poseStack, ModRenderTypes.glow(TEXTURE), (pose, buffer) ->
                model.emit(group, RenderSinks.sink(pose, buffer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY),
                        brightness, brightness, brightness, 1f));
    }

    /** The shield (or cloak) bubble: several additive layers drifting at different speeds. */
    private static void renderBubble(PoseStack poseStack, SubmitNodeCollector collector, float intensity, float wobble, float time) {
        EnergyBubble.Grid grid = EnergyBubble.compute(RenderSinks.view(poseStack.last()), wobble, time);
        for (EnergyBubble.Layer layer : EnergyBubble.LAYERS) {
            RenderType type = ModRenderTypes.energyBubble(SHIELD_TEXTURE, time * layer.uDrift(), time * layer.vDrift());
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> EnergyBubble.emitLayer(grid, layer,
                    RenderSinks.sink(pose, buffer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY), intensity));
        }
    }
}
