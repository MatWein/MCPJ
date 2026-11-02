package de.mcpj.core.render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Triangulated Wavefront OBJ model with named groups ("o"/"g"); faces become triangles, emitted as quads. */
public final class ObjMesh {
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_TRIANGLE = 3;
    private static final int VERTICES_PER_QUAD = 4;
    private static final String DEFAULT_GROUP = "default";
    private static final float[] NO_UV = {0, 0};
    private static final float[] UP = {0, 1, 0};
    private static final float MIN_NORMAL_LENGTH = 1e-9f;

    private final Map<String, float[]> groups;

    private ObjMesh(Map<String, float[]> groups) {
        this.groups = groups;
    }

    public static ObjMesh parse(Reader source) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        Map<String, List<Float>> data = new LinkedHashMap<>();
        List<Float> current = data.computeIfAbsent(DEFAULT_GROUP, key -> new ArrayList<>());
        try (BufferedReader reader = new BufferedReader(source)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "v" -> positions.add(floats(tokens, 3));
                    case "vt" -> uvs.add(floats(tokens, 2));
                    case "vn" -> normals.add(floats(tokens, 3));
                    case "o", "g" -> current = data.computeIfAbsent(tokens.length > 1 ? tokens[1] : DEFAULT_GROUP, key -> new ArrayList<>());
                    case "f" -> addFace(tokens, positions, uvs, normals, current);
                    default -> {}
                }
            }
        }
        Map<String, float[]> groups = new LinkedHashMap<>();
        data.forEach((name, values) -> {
            if (values.isEmpty()) return;
            float[] array = new float[values.size()];
            for (int i = 0; i < array.length; i++) array[i] = values.get(i);
            groups.put(name, array);
        });
        return new ObjMesh(groups);
    }

    public boolean hasGroup(String group) {
        return groups.containsKey(group);
    }

    /** Emits a group's triangles as quads (last corner repeated) with a uniform color. */
    public void emit(String group, VertexSink sink, float red, float green, float blue, float alpha) {
        float[] data = groups.get(group);
        if (data == null) return;
        int triangles = data.length / (FLOATS_PER_VERTEX * VERTICES_PER_TRIANGLE);
        for (int triangle = 0; triangle < triangles; triangle++) {
            int base = triangle * FLOATS_PER_VERTEX * VERTICES_PER_TRIANGLE;
            for (int corner = 0; corner < VERTICES_PER_QUAD; corner++) {
                int o = base + Math.min(corner, VERTICES_PER_TRIANGLE - 1) * FLOATS_PER_VERTEX;
                sink.vertex(data[o], data[o + 1], data[o + 2], red, green, blue, alpha, data[o + 3], data[o + 4],
                        data[o + 5], data[o + 6], data[o + 7]);
            }
        }
    }

    private static float[] floats(String[] tokens, int count) {
        float[] values = new float[count];
        for (int i = 0; i < count; i++) values[i] = Float.parseFloat(tokens[i + 1]);
        return values;
    }

    private static void addFace(String[] tokens, List<float[]> positions, List<float[]> uvs, List<float[]> normals, List<Float> out) {
        int corners = tokens.length - 1;
        int[][] indices = new int[corners][];
        for (int i = 0; i < corners; i++) indices[i] = parseIndex(tokens[i + 1], positions.size(), uvs.size(), normals.size());
        float[] faceNormal = faceNormal(positions, indices);
        for (int i = 1; i < corners - 1; i++) {
            for (int[] vertex : new int[][]{indices[0], indices[i], indices[i + 1]}) {
                float[] position = positions.get(vertex[0]);
                float[] uv = vertex[1] >= 0 ? uvs.get(vertex[1]) : NO_UV;
                float[] normal = vertex[2] >= 0 ? normals.get(vertex[2]) : faceNormal;
                out.add(position[0]); out.add(position[1]); out.add(position[2]);
                out.add(uv[0]); out.add(1.0f - uv[1]);
                out.add(normal[0]); out.add(normal[1]); out.add(normal[2]);
            }
        }
    }

    private static int[] parseIndex(String token, int positionCount, int uvCount, int normalCount) {
        String[] parts = token.split("/");
        int position = resolve(parts[0], positionCount);
        int uv = parts.length > 1 && !parts[1].isEmpty() ? resolve(parts[1], uvCount) : -1;
        int normal = parts.length > 2 && !parts[2].isEmpty() ? resolve(parts[2], normalCount) : -1;
        return new int[]{position, uv, normal};
    }

    private static int resolve(String index, int size) {
        int value = Integer.parseInt(index);
        return value < 0 ? size + value : value - 1;
    }

    private static float[] faceNormal(List<float[]> positions, int[][] indices) {
        float[] a = positions.get(indices[0][0]), b = positions.get(indices[1][0]), c = positions.get(indices[2][0]);
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        return length < MIN_NORMAL_LENGTH ? UP : new float[]{nx / length, ny / length, nz / length};
    }
}
