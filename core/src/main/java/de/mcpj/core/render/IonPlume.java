package de.mcpj.core.render;

import de.mcpj.core.jumper.JumperGeometry;
import de.mcpj.core.math.MathUtil;

/**
 * Ion plume behind a nozzle: two crossed, double-sided ribbons that taper, pulse and grow with speed.
 * Additive; the texture region of the plume is given by the platform (it is part of the ship's atlas).
 */
public final class IonPlume {
    private static final int SEGMENTS = 8;
    private static final float BASE_LENGTH = 2.2f;
    private static final float LENGTH_PER_SPEED = 9f;
    private static final float MAX_SPEED = 1.0f;
    private static final float TAPER = 0.65f;
    private static final float PULSE_BASE = 0.85f;
    private static final float PULSE_AMPLITUDE = 0.15f;
    private static final float PULSE_SPEED = 0.35f;
    private static final float PULSE_PHASE_PER_SEGMENT = 1.1f;
    private static final float PHASE_PER_SIDE = 7f;
    private static final float PHASE_PER_NOZZLE = 13f;
    private static final float START_BEHIND_NOZZLE = 0.12f;

    /** Texture region (u0, v0, u1, v1) of the plume. */
    public record Uv(float u0, float v0, float u1, float v1) {}

    private record Vertex(float x, float y, float z, float u, float v, float brightness) {}

    private IonPlume() {}

    /** Both plumes of one drive pod (model units; the pod's extension is applied by the caller's pose). */
    public static void emitPod(VertexSink sink, Uv uv, int side, float glow, float speed, float time) {
        for (float nozzle : new float[]{-JumperGeometry.NOZZLE_OFFSET_X, JumperGeometry.NOZZLE_OFFSET_X}) {
            emit(sink, uv, side * JumperGeometry.POD_X + nozzle, JumperGeometry.POD_Y, JumperGeometry.POD_REAR_Z - START_BEHIND_NOZZLE,
                    glow, speed, time + side * PHASE_PER_SIDE + nozzle * PHASE_PER_NOZZLE);
        }
    }

    static void emit(VertexSink sink, Uv uv, float centerX, float centerY, float startZ, float glow, float speed, float time) {
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
            float v0 = MathUtil.lerp(t0, uv.v0(), uv.v1());
            float v1 = MathUtil.lerp(t1, uv.v0(), uv.v1());
            float halfHeight = JumperGeometry.NOZZLE_HALF_HEIGHT;
            float halfWidth = JumperGeometry.NOZZLE_HALF_WIDTH;
            doubleSidedQuad(sink,
                    new Vertex(centerX, centerY - halfHeight * width0, z0, uv.u0(), v0, brightness0),
                    new Vertex(centerX, centerY + halfHeight * width0, z0, uv.u1(), v0, brightness0),
                    new Vertex(centerX, centerY + halfHeight * width1, z1, uv.u1(), v1, brightness1),
                    new Vertex(centerX, centerY - halfHeight * width1, z1, uv.u0(), v1, brightness1));
            doubleSidedQuad(sink,
                    new Vertex(centerX - halfWidth * width0, centerY, z0, uv.u0(), v0, brightness0),
                    new Vertex(centerX + halfWidth * width0, centerY, z0, uv.u1(), v0, brightness0),
                    new Vertex(centerX + halfWidth * width1, centerY, z1, uv.u1(), v1, brightness1),
                    new Vertex(centerX - halfWidth * width1, centerY, z1, uv.u0(), v1, brightness1));
        }
    }

    private static float pulse(float time, int segment) {
        return PULSE_BASE + PULSE_AMPLITUDE * (float) Math.sin(time * PULSE_SPEED - segment * PULSE_PHASE_PER_SEGMENT);
    }

    private static void doubleSidedQuad(VertexSink sink, Vertex a, Vertex b, Vertex c, Vertex d) {
        for (Vertex vertex : new Vertex[]{a, b, c, d, d, c, b, a}) {
            sink.vertex(vertex.x, vertex.y, vertex.z, vertex.brightness, vertex.brightness, vertex.brightness, 1f,
                    vertex.u, vertex.v, 0, 1, 0);
        }
    }
}
