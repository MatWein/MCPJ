package de.mcpj.mc.client.render;

import de.mcpj.mc.Mcpj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.mcpj.core.render.DroneLook;
import de.mcpj.core.render.ObjMesh;
import de.mcpj.mc.util.Convert;
import de.mcpj.mc.entity.drone.DroneEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DroneRenderer extends EntityRenderer<DroneEntity, DroneRenderer.State> {
    private static final Identifier TEXTURE = Mcpj.id("textures/entity/drone.png");
    private static final int FULL_BLOCK_LIGHT = 15;

    public static final class State extends EntityRenderState {
        DroneLook.@Nullable Orientation orientation;
    }

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DroneEntity drone, State state, float partialTick) {
        super.extractRenderState(drone, state, partialTick);
        Vec3 moved = drone.position().subtract(drone.xo, drone.yo, drone.zo);
        state.orientation = DroneLook.orientation(Convert.toCore(moved), Convert.toCore(drone.getDeltaMovement()), state.ageInTicks);
    }

    @Override
    protected int getBlockLightLevel(DroneEntity entity, BlockPos pos) {
        return FULL_BLOCK_LIGHT;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ObjMesh model = ModelCache.drone();
        DroneLook.Orientation orientation = state.orientation;
        if (model == null || orientation == null) return;
        poseStack.pushPose();
        poseStack.translate(0, DroneLook.CENTER_HEIGHT, 0);
        poseStack.rotate(Axis.YP, orientation.yaw());
        poseStack.rotate(Axis.XP, orientation.pitch());
        poseStack.rotateDegrees(Axis.ZP, orientation.spinDegrees());
        poseStack.scale(DroneLook.SCALE, DroneLook.SCALE, DroneLook.SCALE);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) ->
                model.emit("drone", RenderSinks.sink(pose, buffer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY), 1, 1, 1, 1));
        float pulse = DroneLook.glowPulse(state.ageInTicks);
        collector.submitCustomGeometry(poseStack, RenderTypes.eyes(TEXTURE), (pose, buffer) ->
                model.emit("drone_glow", RenderSinks.sink(pose, buffer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY), pulse, pulse, pulse, 1));
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
