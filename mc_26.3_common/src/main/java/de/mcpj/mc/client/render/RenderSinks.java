package de.mcpj.mc.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.mcpj.core.render.VertexSink;
import de.mcpj.core.render.ViewTransform;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** The core's vertex output and view transform on top of Minecraft's pose stack and vertex consumers. */
final class RenderSinks {
    private RenderSinks() {}

    static VertexSink sink(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay) {
        return (x, y, z, red, green, blue, alpha, u, v, normalX, normalY, normalZ) -> consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    static ViewTransform view(PoseStack.Pose pose) {
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Vector4f position = new Vector4f();
        Vector3f normal = new Vector3f();
        return new ViewTransform() {
            @Override
            public void position(float x, float y, float z, float[] out) {
                matrix.transform(position.set(x, y, z, 1f));
                out[0] = position.x;
                out[1] = position.y;
                out[2] = position.z;
            }

            @Override
            public void normal(float x, float y, float z, float[] out) {
                normalMatrix.transform(normal.set(x, y, z));
                out[0] = normal.x;
                out[1] = normal.y;
                out[2] = normal.z;
            }
        };
    }
}
