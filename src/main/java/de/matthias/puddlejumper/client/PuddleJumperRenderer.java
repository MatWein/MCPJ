package de.matthias.puddlejumper.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;
import static de.matthias.puddlejumper.entity.PuddleJumperEntity.POD_REAR_Z;
import static de.matthias.puddlejumper.entity.PuddleJumperEntity.POD_X;
import static de.matthias.puddlejumper.entity.PuddleJumperEntity.POD_Y;

public class PuddleJumperRenderer extends EntityRenderer<PuddleJumperEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/puddle_jumper.png");
    public static final ResourceLocation SHIELD_TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/shield.png");
    /** Blase um den Jumper (Halbachsen in Blöcken, Mittelpunkt über dem Boden). */
    private static final float BUBBLE_RX = 3.1f, BUBBLE_RY = 2.0f, BUBBLE_RZ = 5.5f, BUBBLE_CY = 1.0f;
    private static final int BUBBLE_LON = 40, BUBBLE_LAT = 20;
    private static final float POD_OUT = 1.6f;
    private static final float SHADOW = 2.2f;

    // Ionenfahne: Atlas-Region (u0, v0, u1, v1), siehe tools/build_model.py REG_PLUME
    private static final float PU0 = 0.751f, PV0 = 0.751f, PU1 = 0.999f, PV1 = 0.999f;
    private static final int PLUME_SEGMENTS = 8;
    /** Düsenfenster relativ zur Gondelmitte (Modelleinheiten). */
    private static final float NOZZLE_OX = 0.38f, NOZZLE_HALF_W = 0.26f, NOZZLE_HALF_H = 0.8f;

    public PuddleJumperRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = SHADOW;
    }

    @Override
    public ResourceLocation getTextureLocation(PuddleJumperEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PuddleJumperEntity e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ObjModel model = ModelCache.jumper;
        if (model == null) return;
        Minecraft mc = Minecraft.getInstance();
        boolean ownShip = mc.player != null && e.hasPassenger(mc.player);
        boolean firstPersonInside = ownShip && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == mc.player;

        // Tarnung: für andere komplett unsichtbar (nur dezente Blase), der Pilot sieht einen Leuchtumriss
        float cloak = Mth.lerp(pt, e.cloakAnimO, e.cloakAnim);
        float shield = Mth.lerp(pt, e.shieldAnimO, e.shieldAnim);
        boolean cloaked = cloak > 0.5f;
        shadowRadius = cloaked ? 0 : SHADOW;

        ps.pushPose();
        float yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));

        // Neigung um den Schwerpunkt; Schweben (leichtes Wabern) nur in der Luft
        float roll = Mth.lerp(pt, e.visRollO, e.visRoll);
        float pitch = Mth.lerp(pt, e.visPitchO, e.visPitch);
        float hurt = e.getHurtTime() - pt;
        float dmg = Math.max(0, e.getDamage() - pt);
        if (hurt > 0) roll += Mth.sin(hurt) * hurt * dmg / 400f * e.getHurtDir();
        float bob = e.onGround() ? 0f : Mth.sin((e.tickCount + pt) * 0.08f) * 0.04f;

        ps.translate(0, 1.0 + bob, 0);
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(roll));
        ps.translate(0, -1.0, 0);

        float s = PuddleJumperEntity.MODEL_SCALE;

        ps.pushPose();
        ps.scale(s, s, s);

        float pods = Mth.lerp(pt, e.podAnimO, e.podAnim);
        RenderType glowType = RenderType.eyes(TEXTURE);
        // getarnt: Pilot sieht sein Schiff als additiven Umriss (schreibt keine Tiefe -> kein Eis-Problem)
        boolean ghost = cloaked && ownShip;
        boolean drawShip = !cloaked || ghost;
        RenderType solid = ghost ? glowType : RenderType.entityCutout(TEXTURE);
        float shipLight = ghost ? 0.22f : 1f;
        int shipLightmap = ghost ? LightTexture.FULL_BRIGHT : light;
        int ov = OverlayTexture.NO_OVERLAY;

        // Rumpf - nicht in der Ego-Perspektive des Piloten (freie Sicht)
        if (drawShip && !firstPersonInside) {
            model.render("hull", ps.last(), buffers.getBuffer(solid), shipLightmap, ov, shipLight, shipLight, shipLight, 1f);
        }

        // Antriebsgondeln
        float out = smooth(pods);
        float glow = Mth.lerp(pt, e.engineGlowO, e.engineGlow);
        float time = e.tickCount + pt;
        for (int side = -1; side <= 1; side += 2) {
            String name = side > 0 ? "pod_left" : "pod_right";
            ps.pushPose();
            ps.translate(side * POD_OUT * out, -0.2f * out, 0);
            if (drawShip) {
                model.render(name, ps.last(), buffers.getBuffer(solid), shipLightmap, ov, shipLight, shipLight, shipLight, 1f);
            }
            if (!cloaked && glow > 0.02f) {
                model.render(name + "_glow", ps.last(), buffers.getBuffer(glowType), LightTexture.FULL_BRIGHT, ov, glow, glow, glow, 1);
                for (float ox : new float[]{-NOZZLE_OX, NOZZLE_OX}) {
                    renderPlume(ps, buffers.getBuffer(glowType), side * POD_X + ox, POD_Y, POD_REAR_Z - 0.12f,
                            glow, (float) e.clientSpeed, time + side * 7f + ox * 13f);
                }
            }
            // Drohnen-Ports leuchten, sobald die Gondeln ausgefahren sind
            if (!cloaked && out > 0.05f) {
                model.render(name + "_ports", ps.last(), buffers.getBuffer(glowType), LightTexture.FULL_BRIGHT, ov, out, out, out, 1);
            }
            ps.popPose();
        }

        ps.popPose(); // Modell-Skalierung

        // Energieblase: Schild deutlich und wabernd, Tarnung nur sehr dezent und ruhig
        float flash = Math.max(0, e.shieldFlash - pt) / 10f;
        float intensity = shield * (0.55f + 0.6f * flash) + cloak * (1 - shield) * (ownShip ? 0.2f : 0.09f);
        if (intensity > 0.005f) {
            renderBubble(ps, buffers, intensity, 0.02f * shield, time);
        }

        ps.popPose();
        super.render(e, entityYaw, pt, ps, buffers, light);
    }

    /**
     * Energieblase mit Wasser-Effekt: zwei Lagen der verschwommenen Kaustik-Textur driften in verschiedene
     * Richtungen (Interferenz wie Lichtspiel auf Wasser), der Rand leuchtet stärker als die Mitte (Fresnel),
     * die Hülle wabert sanft. Additiv und ohne Tiefenschreiben.
     */
    private static void renderBubble(PoseStack ps, MultiBufferSource buffers, float intensity, float wobble, float time) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();

        // Gitter einmal berechnen: Position, Normale, Fresnel-Faktor (Blickwinkel im Kameraraum)
        int cols = BUBBLE_LON + 1, rows = BUBBLE_LAT + 1;
        float[][] pos = new float[rows * cols][];
        float[] fres = new float[rows * cols];
        Vector4f vp = new Vector4f();
        Vector3f vn = new Vector3f();
        for (int la = 0; la < rows; la++) {
            for (int lo = 0; lo < cols; lo++) {
                float[] p = bubblePoint(la, lo, wobble, time);
                int i = la * cols + lo;
                pos[i] = p;
                // Normale des Ellipsoids: (x/rx², (y-cy)/ry², z/rz²)
                vn.set(p[0] / (BUBBLE_RX * BUBBLE_RX), (p[1] - BUBBLE_CY) / (BUBBLE_RY * BUBBLE_RY), p[2] / (BUBBLE_RZ * BUBBLE_RZ));
                n.transform(vn).normalize();
                m.transform(vp.set(p[0], p[1], p[2], 1f));
                float len = (float) Math.sqrt(vp.x * vp.x + vp.y * vp.y + vp.z * vp.z) + 1e-4f;
                float facing = Math.abs((vp.x * vn.x + vp.y * vn.y + vp.z * vn.z) / len);
                float f = 1f - facing;
                fres[i] = 0.2f + 0.8f * f * f;
            }
        }

        float[][] layers = {
                // u-Drift, v-Drift, UV-Skalierung, Gewicht
                {time * 0.0021f, time * 0.0013f, 2.0f, 0.6f},
                {-time * 0.0016f, time * 0.0024f, 2.7f, 0.5f},
        };
        for (float[] layer : layers) {
            VertexConsumer vc = buffers.getBuffer(ModRenderTypes.bubble(SHIELD_TEXTURE, layer[0], layer[1]));
            float sc = layer[2], weight = intensity * layer[3];
            for (int la = 0; la < BUBBLE_LAT; la++) {
                for (int lo = 0; lo < BUBBLE_LON; lo++) {
                    int[] idx = {la * cols + lo, la * cols + lo + 1, (la + 1) * cols + lo + 1, (la + 1) * cols + lo};
                    float[][] uv = {
                            {lo * sc / BUBBLE_LON * 2f, la * sc / BUBBLE_LAT},
                            {(lo + 1) * sc / BUBBLE_LON * 2f, la * sc / BUBBLE_LAT},
                            {(lo + 1) * sc / BUBBLE_LON * 2f, (la + 1) * sc / BUBBLE_LAT},
                            {lo * sc / BUBBLE_LON * 2f, (la + 1) * sc / BUBBLE_LAT}};
                    // Reihenfolge (lo, lo+1) x (la, la+1) ergibt nach der Rechte-Hand-Regel eine nach außen
                    // zeigende Vorderseite; Rückseiten werden verworfen
                    for (int k = 0; k < 4; k++) {
                        int q = k;
                        float[] p = pos[idx[q]];
                        float w = weight * fres[idx[q]];
                        vc.vertex(m, p[0], p[1], p[2]).color(0.6f * w, 0.85f * w, w, 1f).uv(uv[q][0], uv[q][1])
                                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                                .normal(n, p[0], p[1] - BUBBLE_CY, p[2]).endVertex();
                    }
                }
            }
        }
    }

    private static float[] bubblePoint(int la, int lo, float wobble, float time) {
        float theta = (float) Math.PI * la / BUBBLE_LAT;
        float phi = (float) (2 * Math.PI) * (lo % BUBBLE_LON) / BUBBLE_LON;
        // zwei langsame, gegenläufige Wellen -> weiches Wabern statt Pulsieren
        float w = 1f + wobble * (Mth.sin(time * 0.07f + theta * 4f + phi * 2f) + 0.6f * Mth.sin(-time * 0.05f + theta * 3f - phi * 3f));
        float st = Mth.sin(theta);
        return new float[]{BUBBLE_RX * st * Mth.cos(phi) * w, BUBBLE_CY + BUBBLE_RY * Mth.cos(theta) * w, BUBBLE_RZ * st * Mth.sin(phi) * w};
    }

    /**
     * Ionenfahne hinter einem Düsenfenster: zwei gekreuzte, sich verjüngende Flächen (additiv),
     * Länge wächst mit dem Tempo. Sanfte Helligkeitswellen laufen nach hinten - kein Texturflimmern.
     */
    private static void renderPlume(PoseStack ps, VertexConsumer vc, float cx, float cy, float z0,
                                    float glow, float speed, float time) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float length = 2.2f + Math.min(speed, 1.0f) * 9f;
        for (int i = 0; i < PLUME_SEGMENTS; i++) {
            float t0 = (float) i / PLUME_SEGMENTS, t1 = (float) (i + 1) / PLUME_SEGMENTS;
            float za = z0 - length * t0, zb = z0 - length * t1;
            float wa = 1f - 0.65f * t0, wb = 1f - 0.65f * t1;
            float ca = glow * (0.85f + 0.15f * Mth.sin(time * 0.35f - i * 1.1f));
            float cb = glow * (0.85f + 0.15f * Mth.sin(time * 0.35f - (i + 1) * 1.1f));
            float va = Mth.lerp(t0, PV0, PV1), vb = Mth.lerp(t1, PV0, PV1);
            // senkrechte Fläche (Höhe des Düsenfensters)
            quad(vc, m, n,
                    cx, cy - NOZZLE_HALF_H * wa, za, PU0, va, ca,
                    cx, cy + NOZZLE_HALF_H * wa, za, PU1, va, ca,
                    cx, cy + NOZZLE_HALF_H * wb, zb, PU1, vb, cb,
                    cx, cy - NOZZLE_HALF_H * wb, zb, PU0, vb, cb);
            // waagerechte Fläche (Breite des Düsenfensters)
            quad(vc, m, n,
                    cx - NOZZLE_HALF_W * wa, cy, za, PU0, va, ca,
                    cx + NOZZLE_HALF_W * wa, cy, za, PU1, va, ca,
                    cx + NOZZLE_HALF_W * wb, cy, zb, PU1, vb, cb,
                    cx - NOZZLE_HALF_W * wb, cy, zb, PU0, vb, cb);
        }
    }

    /** Doppelseitiges Quad (eyes-RenderType verwirft Rückseiten). */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float x0, float y0, float z0, float u0, float v0, float c0,
                             float x1, float y1, float z1, float u1, float v1, float c1,
                             float x2, float y2, float z2, float u2, float v2, float c2,
                             float x3, float y3, float z3, float u3, float v3, float c3) {
        vert(vc, m, n, x0, y0, z0, u0, v0, c0);
        vert(vc, m, n, x1, y1, z1, u1, v1, c1);
        vert(vc, m, n, x2, y2, z2, u2, v2, c2);
        vert(vc, m, n, x3, y3, z3, u3, v3, c3);
        vert(vc, m, n, x3, y3, z3, u3, v3, c3);
        vert(vc, m, n, x2, y2, z2, u2, v2, c2);
        vert(vc, m, n, x1, y1, z1, u1, v1, c1);
        vert(vc, m, n, x0, y0, z0, u0, v0, c0);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, float c) {
        vc.vertex(m, x, y, z).color(c, c, c, 1f).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 1, 0).endVertex();
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }
}
