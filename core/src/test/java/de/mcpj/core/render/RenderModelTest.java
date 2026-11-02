package de.mcpj.core.render;

import de.mcpj.core.config.DefaultConfigBuilder;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderModelTest {
    private static final ViewTransform IDENTITY_AT_DISTANCE = new ViewTransform() {
        @Override public void position(float x, float y, float z, float[] out) { out[0] = x; out[1] = y; out[2] = z - 20; }
        @Override public void normal(float x, float y, float z, float[] out) { out[0] = x; out[1] = y; out[2] = z; }
    };

    @Test
    void objQuadsBecomeTwoTrianglesEmittedAsQuads() throws Exception {
        String obj = """
                o panel
                v 0 0 0
                v 1 0 0
                v 1 1 0
                v 0 1 0
                vt 0 0
                vt 1 1
                f 1/1 2/2 3/2 4/1
                """;
        ObjMesh mesh = ObjMesh.parse(new StringReader(obj));
        assertTrue(mesh.hasGroup("panel"));
        List<float[]> vertices = new ArrayList<>();
        mesh.emit("panel", (x, y, z, r, g, b, a, u, v, nx, ny, nz) -> vertices.add(new float[]{x, y, z, v, nz}), 1, 1, 1, 1);
        assertEquals(8, vertices.size(), "2 triangles as 2 quads");
        assertEquals(1f, vertices.get(0)[3], "v is flipped to texture space");
        assertEquals(1f, vertices.get(0)[4], 1e-6, "face normal points along +z");
    }

    @Test
    void bubbleAndPlumeEmitTheirQuads() {
        DefaultConfigBuilder.useDefaults();
        int[] count = {0};
        EnergyBubble.Grid grid = EnergyBubble.compute(IDENTITY_AT_DISTANCE, 0.02f, 10f);
        EnergyBubble.emitLayer(grid, EnergyBubble.LAYERS.get(0), (x, y, z, r, g, b, a, u, v, nx, ny, nz) -> count[0]++, 1f);
        assertEquals(40 * 20 * 4, count[0]);
        count[0] = 0;
        IonPlume.emitPod((x, y, z, r, g, b, a, u, v, nx, ny, nz) -> count[0]++, new IonPlume.Uv(0, 0, 1, 1), 1, 1f, 0.5f, 3f);
        assertEquals(2 * 8 * 2 * 8, count[0], "2 nozzles, 8 segments, 2 crossed double-sided quads");
        assertFalse(EnergyBubble.isVisible(EnergyBubble.intensity(0, 0, 0, false)));
        assertTrue(EnergyBubble.isVisible(EnergyBubble.intensity(1, 0, 0, false)));
    }
}
