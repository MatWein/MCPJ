package de.matthias.puddlejumper.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimaler OBJ-Loader (v/vt/vn/f, Gruppen über "o"/"g"). Jede Gruppe wird als flaches Float-Array
 * gespeichert (x,y,z,u,v,nx,ny,nz pro Vertex) und als degenerierte Quads (Dreieck + doppelter Vertex)
 * in einen Entity-VertexConsumer geschrieben.
 */
public final class ObjModel {
    private static final int STRIDE = 8;
    private final Map<String, float[]> groups;

    private ObjModel(Map<String, float[]> groups) {
        this.groups = groups;
    }

    public boolean has(String group) {
        return groups.containsKey(group);
    }

    public Iterable<String> groupNames() {
        return groups.keySet();
    }

    public static ObjModel load(ResourceManager rm, ResourceLocation loc) throws IOException {
        List<float[]> pos = new ArrayList<>(), uv = new ArrayList<>(), nrm = new ArrayList<>();
        Map<String, List<Float>> data = new LinkedHashMap<>();
        List<Float> cur = data.computeIfAbsent("default", k -> new ArrayList<>());
        try (BufferedReader r = new BufferedReader(new InputStreamReader(rm.open(loc), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\s+");
                switch (p[0]) {
                    case "v" -> pos.add(new float[]{f(p[1]), f(p[2]), f(p[3])});
                    case "vt" -> uv.add(new float[]{f(p[1]), f(p[2])});
                    case "vn" -> nrm.add(new float[]{f(p[1]), f(p[2]), f(p[3])});
                    case "o", "g" -> cur = data.computeIfAbsent(p.length > 1 ? p[1] : "default", k -> new ArrayList<>());
                    case "f" -> {
                        int n = p.length - 1;
                        int[][] idx = new int[n][];
                        for (int i = 0; i < n; i++) idx[i] = parseIndex(p[i + 1], pos.size(), uv.size(), nrm.size());
                        float[] faceN = faceNormal(pos, idx);
                        for (int i = 1; i < n - 1; i++) {
                            for (int[] v : new int[][]{idx[0], idx[i], idx[i + 1]}) {
                                float[] pp = pos.get(v[0]);
                                float[] tt = v[1] >= 0 ? uv.get(v[1]) : new float[]{0, 0};
                                float[] nn = v[2] >= 0 ? nrm.get(v[2]) : faceN;
                                cur.add(pp[0]); cur.add(pp[1]); cur.add(pp[2]);
                                cur.add(tt[0]); cur.add(1.0f - tt[1]); // OBJ: v von unten
                                cur.add(nn[0]); cur.add(nn[1]); cur.add(nn[2]);
                            }
                        }
                    }
                    default -> {}
                }
            }
        }
        Map<String, float[]> out = new LinkedHashMap<>();
        data.forEach((k, list) -> {
            if (list.isEmpty()) return;
            float[] arr = new float[list.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
            out.put(k, arr);
        });
        return new ObjModel(out);
    }

    private static float f(String s) {
        return Float.parseFloat(s);
    }

    private static int[] parseIndex(String token, int np, int nt, int nn) {
        String[] s = token.split("/");
        int v = resolve(s[0], np);
        int t = s.length > 1 && !s[1].isEmpty() ? resolve(s[1], nt) : -1;
        int n = s.length > 2 && !s[2].isEmpty() ? resolve(s[2], nn) : -1;
        return new int[]{v, t, n};
    }

    private static int resolve(String s, int size) {
        int i = Integer.parseInt(s);
        return i < 0 ? size + i : i - 1;
    }

    private static float[] faceNormal(List<float[]> pos, int[][] idx) {
        float[] a = pos.get(idx[0][0]), b = pos.get(idx[1][0]), c = pos.get(idx[2][0]);
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        return l < 1e-9f ? new float[]{0, 1, 0} : new float[]{nx / l, ny / l, nz / l};
    }

    public void render(String group, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay,
                       float r, float g, float b, float a) {
        float[] d = groups.get(group);
        if (d == null) return;
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int tris = d.length / (STRIDE * 3);
        for (int t = 0; t < tris; t++) {
            int base = t * STRIDE * 3;
            for (int k = 0; k < 4; k++) {
                int o = base + Math.min(k, 2) * STRIDE; // 4. Vertex = 3. Vertex (degeneriertes Quad)
                vc.vertex(m, d[o], d[o + 1], d[o + 2])
                        .color(r, g, b, a)
                        .uv(d[o + 3], d[o + 4])
                        .overlayCoords(overlay)
                        .uv2(light)
                        .normal(n, d[o + 5], d[o + 6], d[o + 7])
                        .endVertex();
            }
        }
    }
}
