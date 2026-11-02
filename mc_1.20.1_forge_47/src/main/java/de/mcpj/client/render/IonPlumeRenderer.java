package de.mcpj.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.mcpj.core.jumper.JumperGeometry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

final class IonPlumeRenderer {
    // Atlas region of the plume in textures/entity/puddle_jumper.png (pixels 768..1024 x 1536..2048)
    private static final float U0 = 0.751f;
    private static final float V0 = 0.751f;
    private static final float U1 = 0.999f;
    private static final float V1 = 0.999f;
    private static final int SEGMENTS = 8;
    private static final float BASE_LENGTH = 2.2f;
    private static final float LENGTH_PER_SPEED = 9f;
    private static final float MAX_SPEED = 1.0f;
    private static final float TAPER = 0.65f;
    private static final float PULSE_BASE = 0.85f;
    private static final float PULSE_AMPLITUDE = 0.15f;
    private static final float PULSE_SPEED = 0.35f;
    private static final float PULSE_PHASE_PER_SEGMENT = 1.1f;

    private record Vertex(float x, float y, float z, float u, float v, float brightness) {}

    private IonPlumeRenderer() {}

    static void render(PoseStack poseStack, VertexConsumer consumer, float centerX, float centerY, float startZ,
                       float glow, float speed, float time) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float length = BASE_LENGTH + Math.min(speed, MAX_SPEED) * LENGTH_PER_SPEED;
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float t0 = (float) segment / SEGMENTS;
            float t1 = (float) (segment + 1) / SEGMENTS;
            float z0 = startZ - length * t0;
            float z1 = startZ - length * t1;
            float width0 = 1f - TAPER * t0;
            float width1 = 1f - TAPER * t1;
            float brightness0 = glow * pulse(time, segment);
            float brightness1 = glow * pulse(time, segment + 1);
            float v0 = Mth.lerp(t0, V0, V1);
            float v1 = Mth.lerp(t1, V0, V1);
            float halfHeight = JumperGeometry.NOZZLE_HALF_HEIGHT;
            float halfWidth = JumperGeometry.NOZZLE_HALF_WIDTH;
            doubleSidedQuad(consumer, pose, normal,
                    new Vertex(centerX, centerY - halfHeight * width0, z0, U0, v0, brightness0),
                    new Vertex(centerX, centerY + halfHeight * width0, z0, U1, v0, brightness0),
                    new Vertex(centerX, centerY + halfHeight * width1, z1, U1, v1, brightness1),
                    new Vertex(centerX, centerY - halfHeight * width1, z1, U0, v1, brightness1));
            doubleSidedQuad(consumer, pose, normal,
                    new Vertex(centerX - halfWidth * width0, centerY, z0, U0, v0, brightness0),
                    new Vertex(centerX + halfWidth * width0, centerY, z0, U1, v0, brightness0),
                    new Vertex(centerX + halfWidth * width1, centerY, z1, U1, v1, brightness1),
                    new Vertex(centerX - halfWidth * width1, centerY, z1, U0, v1, brightness1));
        }
    }

    private static float pulse(float time, int segment) {
        return PULSE_BASE + PULSE_AMPLITUDE * Mth.sin(time * PULSE_SPEED - segment * PULSE_PHASE_PER_SEGMENT);
    }

    private static void doubleSidedQuad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Vertex a, Vertex b, Vertex c, Vertex d) {
        for (Vertex vertex : new Vertex[]{a, b, c, d, d, c, b, a}) {
            consumer.vertex(pose, vertex.x, vertex.y, vertex.z)
                    .color(vertex.brightness, vertex.brightness, vertex.brightness, 1f)
                    .uv(vertex.u, vertex.v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(normal, 0, 1, 0)
                    .endVertex();
        }
    }
}
