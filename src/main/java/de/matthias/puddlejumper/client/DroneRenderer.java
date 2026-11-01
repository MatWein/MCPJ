package de.matthias.puddlejumper.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.matthias.puddlejumper.entity.DroneEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;

public class DroneRenderer extends EntityRenderer<DroneEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/drone.png");

    public DroneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(DroneEntity entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(DroneEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public void render(DroneEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ObjModel model = ModelCache.drone;
        if (model == null) return;
        Vec3 v = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
        if (v.lengthSqr() < 1.0e-6) v = e.getDeltaMovement();
        float yawR = (float) Mth.atan2(v.x, v.z);
        float pitchR = (float) Mth.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z));

        ps.pushPose();
        ps.translate(0, 0.25, 0);
        ps.mulPose(Axis.YP.rotation(yawR));
        ps.mulPose(Axis.XP.rotation(-pitchR));
        ps.mulPose(Axis.ZP.rotationDegrees((e.tickCount + pt) * 25f)); // Drall
        ps.scale(0.42f, 0.42f, 0.42f);
        model.render("drone", ps.last(), buffers.getBuffer(RenderType.entityCutout(TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        float pulse = 0.7f + 0.3f * Mth.sin((e.tickCount + pt) * 0.9f);
        model.render("drone_glow", ps.last(), buffers.getBuffer(RenderType.eyes(TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1);
        ps.popPose();
        super.render(e, yaw, pt, ps, buffers, light);
    }
}
