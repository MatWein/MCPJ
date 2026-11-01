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

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;
import static de.matthias.puddlejumper.entity.PuddleJumperEntity.POD_REAR_Z;
import static de.matthias.puddlejumper.entity.PuddleJumperEntity.POD_X;
import static de.matthias.puddlejumper.entity.PuddleJumperEntity.POD_Y;

public class PuddleJumperRenderer extends EntityRenderer<PuddleJumperEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/puddle_jumper.png");
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

        // Tarnung: Pilot sieht ein Geisterbild, alle anderen einen schwachen, bläulichen Schimmer
        float alpha = 1f;
        float tintR = 1f, tintG = 1f, tintB = 1f;
        if (e.isCloaked()) {
            float shimmer = 0.03f * Mth.sin((e.tickCount + pt) * 0.15f);
            alpha = ownShip ? 0.3f : (float) Math.min(0.22, 0.1 + shimmer + e.clientSpeed * 0.1);
            tintR = 0.75f;
            tintG = 0.88f;
        }
        shadowRadius = e.isCloaked() ? 0 : SHADOW;

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
        ps.scale(s, s, s);

        float pods = Mth.lerp(pt, e.podAnimO, e.podAnim);
        boolean translucent = alpha < 1f;
        RenderType solid = translucent ? RenderType.entityTranslucent(TEXTURE) : RenderType.entityCutout(TEXTURE);
        RenderType glowType = RenderType.eyes(TEXTURE);
        int ov = OverlayTexture.NO_OVERLAY;

        // Rumpf - nicht in der Ego-Perspektive des Piloten (freie Sicht)
        if (!firstPersonInside) {
            model.render("hull", ps.last(), buffers.getBuffer(solid), light, ov, tintR, tintG, tintB, alpha);
        }

        // Antriebsgondeln
        float out = smooth(pods);
        float glow = Mth.lerp(pt, e.engineGlowO, e.engineGlow);
        float time = e.tickCount + pt;
        for (int side = -1; side <= 1; side += 2) {
            String name = side > 0 ? "pod_left" : "pod_right";
            ps.pushPose();
            ps.translate(side * POD_OUT * out, -0.2f * out, 0);
            model.render(name, ps.last(), buffers.getBuffer(solid), light, ov, tintR, tintG, tintB, alpha);
            if (!translucent && glow > 0.02f) {
                model.render(name + "_glow", ps.last(), buffers.getBuffer(glowType), LightTexture.FULL_BRIGHT, ov, glow, glow, glow, 1);
                for (float ox : new float[]{-NOZZLE_OX, NOZZLE_OX}) {
                    renderPlume(ps, buffers.getBuffer(glowType), side * POD_X + ox, POD_Y, POD_REAR_Z - 0.12f,
                            glow, (float) e.clientSpeed, time + side * 7f + ox * 13f);
                }
            }
            // Drohnen-Ports leuchten, sobald die Gondeln ausgefahren sind
            if (!translucent && out > 0.05f) {
                model.render(name + "_ports", ps.last(), buffers.getBuffer(glowType), LightTexture.FULL_BRIGHT, ov, out, out, out, 1);
            }
            ps.popPose();
        }

        // Positionslichter am Rumpf
        if (!translucent && !firstPersonInside) {
            float lights = e.isVehicle() ? 1f : 0.35f;
            model.render("hull_glow", ps.last(), buffers.getBuffer(glowType), LightTexture.FULL_BRIGHT, ov, lights, lights, lights, 1);
        }

        ps.popPose();
        super.render(e, entityYaw, pt, ps, buffers, light);
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
