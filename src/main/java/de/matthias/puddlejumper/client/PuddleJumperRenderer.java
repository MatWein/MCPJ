package de.matthias.puddlejumper.client;

import com.mojang.blaze3d.vertex.PoseStack;
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

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;

public class PuddleJumperRenderer extends EntityRenderer<PuddleJumperEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/puddle_jumper.png");
    // Scharnier der Heckklappe (Modelleinheiten), muss zu tools/preview.py passen
    private static final float HINGE_Y = 0.19f, HINGE_Z = -10.5f, HATCH_ANGLE = 62f;
    private static final float POD_OUT = 1.6f;
    private static final float SHADOW = 2.2f;

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

        // Tarnung: Pilot sieht ein Geisterbild, alle anderen (fast) nichts
        float alpha = 1f;
        if (e.isCloaked()) {
            alpha = ownShip ? 0.3f : (float) Math.min(0.08, e.clientSpeed * 0.06);
            if (alpha < 0.01f) {
                shadowRadius = 0;
                return;
            }
        }
        shadowRadius = e.isCloaked() ? 0 : SHADOW;

        ps.pushPose();
        float yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));

        // Neigung um den Schwerpunkt
        float roll = Mth.lerp(pt, e.visRollO, e.visRoll);
        float pitch = Mth.lerp(pt, e.visPitchO, e.visPitch);
        float hurt = e.getHurtTime() - pt;
        float dmg = Math.max(0, e.getDamage() - pt);
        if (hurt > 0) roll += Mth.sin(hurt) * hurt * dmg / 400f * e.getHurtDir();
        float bob = (e.isVehicle() && !e.onGround()) ? Mth.sin((e.tickCount + pt) * 0.08f) * 0.04f : 0f;

        ps.translate(0, 1.0 + bob, 0);
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(roll));
        ps.translate(0, -1.0, 0);

        float s = PuddleJumperEntity.MODEL_SCALE;
        ps.scale(s, s, s);

        float hatch = Mth.lerp(pt, e.hatchAnimO, e.hatchAnim);
        float pods = Mth.lerp(pt, e.podAnimO, e.podAnim);
        boolean translucent = alpha < 1f;
        RenderType solid = translucent ? RenderType.entityTranslucent(TEXTURE) : RenderType.entityCutout(TEXTURE);
        int ov = OverlayTexture.NO_OVERLAY;

        // Rumpf, Innenraum, Klappe - nicht in der Ego-Perspektive des Piloten (freie Sicht)
        if (!firstPersonInside) {
            model.render("hull", ps.last(), buffers.getBuffer(solid), light, ov, 1, 1, 1, alpha);
            if (!translucent) model.render("interior", ps.last(), buffers.getBuffer(solid), light, ov, 1, 1, 1, 1);

            ps.pushPose();
            ps.translate(0, HINGE_Y, HINGE_Z);
            ps.mulPose(Axis.XP.rotationDegrees(-HATCH_ANGLE * smooth(hatch)));
            ps.translate(0, -HINGE_Y, -HINGE_Z);
            model.render("hatch", ps.last(), buffers.getBuffer(solid), light, ov, 1, 1, 1, alpha);
            ps.popPose();
        }

        // Antriebsgondeln
        float out = smooth(pods);
        for (int side = -1; side <= 1; side += 2) {
            String name = side > 0 ? "pod_left" : "pod_right";
            ps.pushPose();
            ps.translate(side * POD_OUT * out, -0.2f * out, 0);
            model.render(name, ps.last(), buffers.getBuffer(solid), light, ov, 1, 1, 1, alpha);
            if (!translucent) {
                float glow = e.isVehicle() ? 0.75f + 0.25f * Mth.sin((e.tickCount + pt) * 0.3f) : 0.2f;
                model.render(name + "_glow", ps.last(), buffers.getBuffer(RenderType.eyes(TEXTURE)),
                        LightTexture.FULL_BRIGHT, ov, glow, glow, glow, 1);
            }
            ps.popPose();
        }

        // Leuchtende Details am Rumpf
        if (!translucent && !firstPersonInside) {
            float lights = e.isVehicle() ? 1f : 0.35f;
            model.render("hull_glow", ps.last(), buffers.getBuffer(RenderType.eyes(TEXTURE)), LightTexture.FULL_BRIGHT, ov, lights, lights, lights, 1);
            model.render("interior_glow", ps.last(), buffers.getBuffer(RenderType.eyes(TEXTURE)), LightTexture.FULL_BRIGHT, ov, lights, lights, lights, 1);
        }

        ps.popPose();
        super.render(e, entityYaw, pt, ps, buffers, light);
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }
}
