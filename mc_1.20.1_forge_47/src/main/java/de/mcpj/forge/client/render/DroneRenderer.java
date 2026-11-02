package de.mcpj.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.mcpj.forge.McpjMod;
import de.mcpj.core.render.DroneLook;
import de.mcpj.core.render.ObjMesh;
import de.mcpj.forge.util.Convert;
import de.mcpj.forge.entity.drone.DroneEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class DroneRenderer extends EntityRenderer<DroneEntity> {
    private static final ResourceLocation TEXTURE = McpjMod.id("textures/entity/drone.png");
    private static final int FULL_BLOCK_LIGHT = 15;

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
        ObjMesh model = ModelCache.drone();
        if (model == null) return;
        float time = drone.tickCount + partialTick;
        Vec3 moved = drone.position().subtract(drone.xo, drone.yo, drone.zo);
        DroneLook.Orientation orientation = DroneLook.orientation(Convert.toCore(moved), Convert.toCore(drone.getDeltaMovement()), time);

        poseStack.pushPose();
        poseStack.translate(0, DroneLook.CENTER_HEIGHT, 0);
        poseStack.mulPose(Axis.YP.rotation(orientation.yaw()));
        poseStack.mulPose(Axis.XP.rotation(orientation.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(orientation.spinDegrees()));
        poseStack.scale(DroneLook.SCALE, DroneLook.SCALE, DroneLook.SCALE);
        model.emit("drone", RenderSinks.sink(poseStack.last(), buffers.getBuffer(RenderType.entityCutout(TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY), 1, 1, 1, 1);
        float pulse = DroneLook.glowPulse(time);
        model.emit("drone_glow", RenderSinks.sink(poseStack.last(), buffers.getBuffer(RenderType.eyes(TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY), pulse, pulse, pulse, 1);
        poseStack.popPose();
        super.render(drone, yaw, partialTick, poseStack, buffers, light);
    }
}
