package de.mcpj.mc.client.render;

import de.mcpj.mc.Mcpj;
import com.mojang.blaze3d.vertex.PoseStack;
import de.mcpj.core.render.EnergyBubble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Shield and cloak bubbles are drawn after clouds and weather instead of together with the entities.
 *
 * <p>The bubble is additive and writes no depth (so water and glass behind it stay visible). Drawn with the
 * entities, the clouds rendered later would be blended over it, e.g. when the ship flies above them.
 */
public final class DeferredBubbles {
    private record Bubble(Matrix4f pose, Matrix3f normal, float intensity, float wobble, float time) {}

    private static final ResourceLocation TEXTURE = Mcpj.id("textures/entity/shield.png");
    private static final List<Bubble> PENDING = new ArrayList<>();

    private DeferredBubbles() {}

    /** Remembers the bubble with the current transformation (camera-relative, like all entity rendering). */
    static void add(PoseStack poseStack, float intensity, float wobble, float time) {
        PoseStack.Pose pose = poseStack.last();
        PENDING.add(new Bubble(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()), intensity, wobble, time));
    }

    private static void render(PoseStack.Pose pose, MultiBufferSource buffers, Bubble bubble) {
        EnergyBubble.Grid grid = EnergyBubble.compute(RenderSinks.view(pose), bubble.wobble(), bubble.time());
        for (EnergyBubble.Layer layer : EnergyBubble.LAYERS) {
            RenderType type = ModRenderTypes.energyBubble(TEXTURE, bubble.time() * layer.uDrift(), bubble.time() * layer.vDrift());
            EnergyBubble.emitLayer(grid, layer, RenderSinks.sink(pose, buffers.getBuffer(type), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY), bubble.intensity());
        }
    }

    /** Start of world rendering: drops bubbles of the last frame. */
    public static void clear() {
        PENDING.clear();
    }

    /**
     * After clouds and weather: draws the remembered bubbles. {@code cameraPose} is the camera rotation that the
     * world rendering still has on RenderSystem's model-view stack at this point (it is taken out once), or null if
     * it was already removed (depends on where the loader hooks in).
     */
    public static void renderPending(@Nullable PoseStack.Pose cameraPose) {
        if (PENDING.isEmpty()) return;
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Matrix4f undoCamera = cameraPose != null ? new Matrix4f(cameraPose.pose()).invert() : new Matrix4f();
        Matrix3f undoCameraNormal = cameraPose != null ? new Matrix3f(cameraPose.normal()).invert() : new Matrix3f();
        PoseStack poseStack = new PoseStack();
        for (Bubble bubble : PENDING) {
            poseStack.pushPose();
            poseStack.last().pose().set(undoCamera).mul(bubble.pose());
            poseStack.last().normal().set(undoCameraNormal).mul(bubble.normal());
            render(poseStack.last(), buffers, bubble);
            poseStack.popPose();
        }
        buffers.endBatch();
        PENDING.clear();
    }
}
