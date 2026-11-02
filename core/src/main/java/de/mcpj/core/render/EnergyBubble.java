package de.mcpj.core.render;

import de.mcpj.core.jumper.JumperGeometry;

import java.util.List;

/**
 * The shield and cloak bubble: a gently wobbling ellipsoid around the ship, brighter at its rim (fresnel),
 * drawn in two drifting, additive layers of a caustics texture.
 */
public final class EnergyBubble {
    private static final float RADIUS_X = 3.1f * JumperGeometry.RENDER_SCALE;
    private static final float RADIUS_Y = 2.0f * JumperGeometry.RENDER_SCALE;
    private static final float RADIUS_Z = 5.5f * JumperGeometry.RENDER_SCALE;
    private static final float CENTER_Y = 1.0f * JumperGeometry.RENDER_SCALE;
    private static final int SEGMENTS_AROUND = 40;
    private static final int SEGMENTS_VERTICAL = 20;
    private static final float FRESNEL_BASE = 0.2f;
    private static final float FRESNEL_RIM = 0.8f;
    private static final float MIN_VIEW_DISTANCE = 1e-4f;
    private static final float TINT_RED = 0.5f;
    private static final float TINT_GREEN = 0.78f;
    private static final float WOBBLE_SPEED_A = 0.07f;
    private static final float WOBBLE_SPEED_B = -0.05f;
    private static final float WOBBLE_WEIGHT_B = 0.6f;
    private static final float WAVE_A_VERTICAL = 4f;
    private static final float WAVE_A_AROUND = 2f;
    private static final float WAVE_B_VERTICAL = 3f;
    private static final float WAVE_B_AROUND = -3f;
    private static final float HORIZONTAL_UV_REPEAT = 2f;
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = 2 * PI;
    private static final float SHIELD_BASE_INTENSITY = 0.55f;
    private static final float SHIELD_FLASH_INTENSITY = 0.6f;
    private static final float CLOAK_INTENSITY_OWN = 0.2f;
    private static final float CLOAK_INTENSITY_OTHERS = 0.09f;
    private static final float SHIELD_WOBBLE = 0.02f;
    private static final float MIN_BUBBLE_INTENSITY = 0.005f;

    /** A texture layer: its drift per tick (texture offset, applied by the platform), size and weight. */
    public record Layer(float uDrift, float vDrift, float uvScale, float weight) {}

    public static final List<Layer> LAYERS = List.of(
            new Layer(0.0021f, 0.0013f, 2.0f, 0.6f),
            new Layer(-0.0016f, 0.0024f, 2.7f, 0.5f));

    /** The bubble's surface for one frame, with its fresnel brightness per point. */
    public static final class Grid {
        private final float[][] points;
        private final float[] fresnel;

        private Grid(float[][] points, float[] fresnel) {
            this.points = points;
            this.fresnel = fresnel;
        }
    }

    private EnergyBubble() {}

    public static Grid compute(ViewTransform view, float wobble, float time) {
        int columns = SEGMENTS_AROUND + 1;
        int rows = SEGMENTS_VERTICAL + 1;
        float[][] points = new float[rows * columns][];
        float[] fresnel = new float[rows * columns];
        float[] viewPosition = new float[3];
        float[] normal = new float[3];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                float[] point = surfacePoint(row, column, wobble, time);
                int index = row * columns + column;
                points[index] = point;
                view.normal(point[0] / (RADIUS_X * RADIUS_X), (point[1] - CENTER_Y) / (RADIUS_Y * RADIUS_Y), point[2] / (RADIUS_Z * RADIUS_Z), normal);
                float normalLength = (float) Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
                view.position(point[0], point[1], point[2], viewPosition);
                float distance = (float) Math.sqrt(viewPosition[0] * viewPosition[0] + viewPosition[1] * viewPosition[1]
                        + viewPosition[2] * viewPosition[2]) + MIN_VIEW_DISTANCE;
                float facing = Math.abs((viewPosition[0] * normal[0] + viewPosition[1] * normal[1] + viewPosition[2] * normal[2])
                        / (normalLength > 0 ? normalLength : 1) / distance);
                float rim = 1f - facing;
                fresnel[index] = FRESNEL_BASE + FRESNEL_RIM * rim * rim;
            }
        }
        return new Grid(points, fresnel);
    }

    private static float[] surfacePoint(int row, int column, float wobble, float time) {
        float theta = PI * row / SEGMENTS_VERTICAL;
        float phi = TWO_PI * (column % SEGMENTS_AROUND) / SEGMENTS_AROUND;
        float scale = 1f + wobble * ((float) Math.sin(time * WOBBLE_SPEED_A + theta * WAVE_A_VERTICAL + phi * WAVE_A_AROUND)
                + WOBBLE_WEIGHT_B * (float) Math.sin(time * WOBBLE_SPEED_B + theta * WAVE_B_VERTICAL + phi * WAVE_B_AROUND));
        float sinTheta = (float) Math.sin(theta);
        return new float[]{
                RADIUS_X * sinTheta * (float) Math.cos(phi) * scale,
                CENTER_Y + RADIUS_Y * (float) Math.cos(theta) * scale,
                RADIUS_Z * sinTheta * (float) Math.sin(phi) * scale};
    }

    /** Emits one texture layer of the bubble (quads, bluish additive color). */
    public static void emitLayer(Grid grid, Layer layer, VertexSink sink, float intensity) {
        int columns = SEGMENTS_AROUND + 1;
        float weight = intensity * layer.weight();
        for (int row = 0; row < SEGMENTS_VERTICAL; row++) {
            for (int column = 0; column < SEGMENTS_AROUND; column++) {
                int[] corners = {row * columns + column, row * columns + column + 1, (row + 1) * columns + column + 1, (row + 1) * columns + column};
                int[][] gridUv = {{column, row}, {column + 1, row}, {column + 1, row + 1}, {column, row + 1}};
                for (int corner = 0; corner < corners.length; corner++) {
                    float[] point = grid.points[corners[corner]];
                    float brightness = weight * grid.fresnel[corners[corner]];
                    float u = gridUv[corner][0] * layer.uvScale() / SEGMENTS_AROUND * HORIZONTAL_UV_REPEAT;
                    float v = gridUv[corner][1] * layer.uvScale() / SEGMENTS_VERTICAL;
                    sink.vertex(point[0], point[1], point[2], TINT_RED * brightness, TINT_GREEN * brightness, brightness, 1f,
                            u, v, point[0], point[1] - CENTER_Y, point[2]);
                }
            }
        }
    }

    /** Bubble brightness for shield and cloak fades (0 = invisible). */
    public static float intensity(float shield, float cloak, float shieldFlash, boolean ownShip) {
        float shieldIntensity = shield * (SHIELD_BASE_INTENSITY + SHIELD_FLASH_INTENSITY * shieldFlash);
        float cloakIntensity = cloak * (1 - shield) * (ownShip ? CLOAK_INTENSITY_OWN : CLOAK_INTENSITY_OTHERS);
        return shieldIntensity + cloakIntensity;
    }

    public static float wobble(float shield) {
        return SHIELD_WOBBLE * shield;
    }

    public static boolean isVisible(float intensity) {
        return intensity > MIN_BUBBLE_INTENSITY;
    }

}
