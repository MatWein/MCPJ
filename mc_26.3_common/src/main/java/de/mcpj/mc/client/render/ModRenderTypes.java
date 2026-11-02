package de.mcpj.mc.client.render;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import de.mcpj.mc.Mcpj;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;

/** The shield bubble's render type. */
final class ModRenderTypes {
    /**
     * Like the charged creeper swirl (additive, scrolling texture) but without depth writes,
     * so the bubble does not hide translucent blocks rendered later (ice, water, glass).
     */
    private static final RenderPipeline BUBBLE = RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET)
            .withLocation(Mcpj.id("pipeline/energy_bubble"))
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    private ModRenderTypes() {}

    static RenderType energyBubble(Identifier texture, float uOffset, float vOffset) {
        return RenderType.create("mcpj_bubble", RenderSetup.builder(BUBBLE)
                .withTexture("Sampler0", texture)
                .setTextureTransform(new TextureTransform.OffsetTextureTransform(uOffset, vOffset))
                .useLightmap()
                .sortOnUpload()
                .createRenderSetup());
    }
}
