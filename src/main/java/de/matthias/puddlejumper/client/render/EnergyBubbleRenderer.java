package de.matthias.puddlejumper.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.entity.jumper.JumperGeometry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

final class EnergyBubbleRenderer {
    private static final ResourceLocation TEXTURE = PuddleJumperMod.id("textures/entity/shield.png");
    private static final float RADIUS_X = 3.1f * JumperGeometry.RENDER_SCALE;
    private static final float RADIUS_Y = 2.0f * JumperGeometry.RENDER_SCALE;
    private static final float RADIUS_Z = 5.5f * JumperGeometry.RENDER_SCALE;
    private static final float CENTER_Y = 1.0f * JumperGeometry.RENDER_SCALE;
    private static final int SEGMENTS_AROUND = 40;
    private static final int SEGMENTS_VERTICAL = 20;
    private static final float FRESNEL_BASE = 0.2f;
    private static final float FRESNEL_RIM = 0.8f;
    private static final float MIN_VIEW_DISTANCE = 1e-4f;
    private static final float TINT_RED = 0.6f;
    private static final float TINT_GREEN = 0.85f;
    private static final float WOBBLE_SPEED_A = 0.07f;
    private static final float WOBBLE_SPEED_B = -0.05f;
    private static final float WOBBLE_WEIGHT_B = 0.6f;
    private static final float WAVE_A_VERTICAL = 4f;
    private static final float WAVE_A_AROUND = 2f;
    private static final float WAVE_B_VERTICAL = 3f;
    private static final float WAVE_B_AROUND = -3f;
    private static final float HORIZONTAL_UV_REPEAT = 2f;
    private static final Layer[] LAYERS = {
            new Layer(0.0021f, 0.0013f, 2.0f, 0.6f),
            new Layer(-0.0016f, 0.0024f, 2.7f, 0.5f)};

    private record Layer(float uDrift, float vDrift, float uvScale, float weight) {}

    private EnergyBubbleRenderer() {}

    static void render(PoseStack poseStack, MultiBufferSource buffers, float intensity, float wobble, float time) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        int columns = SEGMENTS_AROUND + 1;
        int rows = SEGMENTS_VERTICAL + 1;
        float[][] points = new float[rows * columns][];
        float[] fresnel = new float[rows * columns];
        computeGrid(pose, normalMatrix, wobble, time, columns, rows, points, fresnel);
        for (Layer layer : LAYERS) {
            VertexConsumer consumer = buffers.getBuffer(ModRenderTypes.energyBubble(TEXTURE, time * layer.uDrift, time * layer.vDrift));
            emitLayer(consumer, pose, normalMatrix, columns, points, fresnel, layer.uvScale, intensity * layer.weight);
        }
    }

    private static void computeGrid(Matrix4f pose, Matrix3f normalMatrix, float wobble, float time, int columns, int rows,
                                    float[][] points, float[] fresnel) {
        Vector4f viewPosition = new Vector4f();
        Vector3f normal = new Vector3f();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                float[] point = surfacePoint(row, column, wobble, time);
                int index = row * columns + column;
                points[index] = point;
                normal.set(point[0] / (RADIUS_X * RADIUS_X), (point[1] - CENTER_Y) / (RADIUS_Y * RADIUS_Y), point[2] / (RADIUS_Z * RADIUS_Z));
                normalMatrix.transform(normal).normalize();
                pose.transform(viewPosition.set(point[0], point[1], point[2], 1f));
                float distance = (float) Math.sqrt(viewPosition.x * viewPosition.x + viewPosition.y * viewPosition.y
                        + viewPosition.z * viewPosition.z) + MIN_VIEW_DISTANCE;
                float facing = Math.abs((viewPosition.x * normal.x + viewPosition.y * normal.y + viewPosition.z * normal.z) / distance);
                float rim = 1f - facing;
                fresnel[index] = FRESNEL_BASE + FRESNEL_RIM * rim * rim;
            }
        }
    }

    private static float[] surfacePoint(int row, int column, float wobble, float time) {
        float theta = Mth.PI * row / SEGMENTS_VERTICAL;
        float phi = Mth.TWO_PI * (column % SEGMENTS_AROUND) / SEGMENTS_AROUND;
        float scale = 1f + wobble * (Mth.sin(time * WOBBLE_SPEED_A + theta * WAVE_A_VERTICAL + phi * WAVE_A_AROUND)
                + WOBBLE_WEIGHT_B * Mth.sin(time * WOBBLE_SPEED_B + theta * WAVE_B_VERTICAL + phi * WAVE_B_AROUND));
        float sinTheta = Mth.sin(theta);
        return new float[]{
                RADIUS_X * sinTheta * Mth.cos(phi) * scale,
                CENTER_Y + RADIUS_Y * Mth.cos(theta) * scale,
                RADIUS_Z * sinTheta * Mth.sin(phi) * scale};
    }

    private static void emitLayer(VertexConsumer consumer, Matrix4f pose, Matrix3f normalMatrix, int columns,
                                  float[][] points, float[] fresnel, float uvScale, float weight) {
        for (int row = 0; row < SEGMENTS_VERTICAL; row++) {
            for (int column = 0; column < SEGMENTS_AROUND; column++) {
                int[] corners = {row * columns + column, row * columns + column + 1, (row + 1) * columns + column + 1, (row + 1) * columns + column};
                int[][] gridUv = {{column, row}, {column + 1, row}, {column + 1, row + 1}, {column, row + 1}};
                for (int corner = 0; corner < corners.length; corner++) {
                    float[] point = points[corners[corner]];
                    float brightness = weight * fresnel[corners[corner]];
                    float u = gridUv[corner][0] * uvScale / SEGMENTS_AROUND * HORIZONTAL_UV_REPEAT;
                    float v = gridUv[corner][1] * uvScale / SEGMENTS_VERTICAL;
                    consumer.vertex(pose, point[0], point[1], point[2])
                            .color(TINT_RED * brightness, TINT_GREEN * brightness, brightness, 1f)
                            .uv(u, v)
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(LightTexture.FULL_BRIGHT)
                            .normal(normalMatrix, point[0], point[1] - CENTER_Y, point[2])
                            .endVertex();
                }
            }
        }
    }
}
