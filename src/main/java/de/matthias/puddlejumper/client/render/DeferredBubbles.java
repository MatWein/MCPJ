package de.matthias.puddlejumper.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.matthias.puddlejumper.PuddleJumperMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
@Mod.EventBusSubscriber(modid = PuddleJumperMod.MOD_ID, value = Dist.CLIENT)
final class DeferredBubbles {
    private record Bubble(Matrix4f pose, Matrix3f normal, float intensity, float wobble, float time) {}

    private static final List<Bubble> PENDING = new ArrayList<>();

    private DeferredBubbles() {}

    /** Remembers the bubble with the current transformation (camera-relative, like all entity rendering). */
    static void add(PoseStack poseStack, float intensity, float wobble, float time) {
        PoseStack.Pose pose = poseStack.last();
        PENDING.add(new Bubble(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()), intensity, wobble, time));
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            PENDING.clear();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER && !PENDING.isEmpty()) {
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            // At this stage the camera rotation is already on RenderSystem's model-view stack (vanilla applies it
            // for clouds and weather), while the remembered entity matrices contain it too: take it out once.
            PoseStack.Pose camera = event.getPoseStack().last();
            Matrix4f undoCamera = new Matrix4f(camera.pose()).invert();
            Matrix3f undoCameraNormal = new Matrix3f(camera.normal()).invert();
            PoseStack poseStack = new PoseStack();
            for (Bubble bubble : PENDING) {
                poseStack.pushPose();
                poseStack.last().pose().set(undoCamera).mul(bubble.pose());
                poseStack.last().normal().set(undoCameraNormal).mul(bubble.normal());
                EnergyBubbleRenderer.render(poseStack, buffers, bubble.intensity(), bubble.wobble(), bubble.time());
                poseStack.popPose();
            }
            buffers.endBatch();
            PENDING.clear();
        }
    }
}
