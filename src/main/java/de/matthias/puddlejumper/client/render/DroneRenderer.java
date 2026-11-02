package de.matthias.puddlejumper.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.entity.drone.DroneEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class DroneRenderer extends EntityRenderer<DroneEntity> {
    private static final ResourceLocation TEXTURE = PuddleJumperMod.id("textures/entity/drone.png");
    private static final int FULL_BLOCK_LIGHT = 15;
    private static final double CENTER_HEIGHT = 0.25;
    private static final float SPIN_DEGREES_PER_TICK = 25f;
    private static final float SCALE = 0.42f;
    private static final float PULSE_BASE = 0.7f;
    private static final float PULSE_AMPLITUDE = 0.3f;
    private static final float PULSE_SPEED = 0.9f;
    private static final double MIN_VELOCITY_SQR = 1.0e-6;

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(DroneEntity entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(DroneEntity entity, BlockPos pos) {
        return FULL_BLOCK_LIGHT;
    }

    @Override
    public void render(DroneEntity drone, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        ObjModel model = ModelCache.drone();
        if (model == null) return;
        Vec3 velocity = drone.position().subtract(drone.xo, drone.yo, drone.zo);
        if (velocity.lengthSqr() < MIN_VELOCITY_SQR) velocity = drone.getDeltaMovement();
        float time = drone.tickCount + partialTick;

        poseStack.pushPose();
        poseStack.translate(0, CENTER_HEIGHT, 0);
        poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(velocity.x, velocity.z)));
        poseStack.mulPose(Axis.XP.rotation((float) -Mth.atan2(velocity.y, velocity.horizontalDistance())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * SPIN_DEGREES_PER_TICK));
        poseStack.scale(SCALE, SCALE, SCALE);
        model.render("drone", poseStack.last(), buffers.getBuffer(RenderType.entityCutout(TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        float pulse = PULSE_BASE + PULSE_AMPLITUDE * Mth.sin(time * PULSE_SPEED);
        model.render("drone_glow", poseStack.last(), buffers.getBuffer(RenderType.eyes(TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1);
        poseStack.popPose();
        super.render(drone, yaw, partialTick, poseStack, buffers, light);
    }
}
