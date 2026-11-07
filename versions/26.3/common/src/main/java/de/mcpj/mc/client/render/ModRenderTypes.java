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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** MCPJ's own render types (glow and shield bubble). */
final class ModRenderTypes {
    /** Fades additive colors out in fog instead of adding the fog color (assets/mcpj/shaders/core/additive.fsh). */
    private static final Identifier ADDITIVE_FRAGMENT_SHADER = Mcpj.id("core/additive");

    /**
     * Like the charged creeper swirl (additive, scrolling texture) but without depth writes,
     * so the bubble does not hide translucent blocks rendered later (ice, water, glass).
     */
    private static final RenderPipeline BUBBLE = RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET)
            .withLocation(Mcpj.id("pipeline/energy_bubble"))
            .withFragmentShader(ADDITIVE_FRAGMENT_SHADER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();

    /**
     * Glowing parts (engines, ion plumes, drone lights, the cloaked ship for its pilot): full bright and additive
     * like vanilla's "eyes" up to 1.21, so dark texels add nothing instead of covering what is behind them.
     */
    private static final RenderPipeline GLOW = RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
            .withLocation(Mcpj.id("pipeline/glow"))
            .withFragmentShader(ADDITIVE_FRAGMENT_SHADER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build();
    private static final Map<Identifier, RenderType> GLOW_TYPES = new ConcurrentHashMap<>();

    private ModRenderTypes() {}

    static RenderType glow(Identifier texture) {
        return GLOW_TYPES.computeIfAbsent(texture, t -> RenderType.create("mcpj_glow", RenderSetup.builder(GLOW)
                .withTexture("Sampler0", t)
                .sortOnUpload()
                .createRenderSetup()));
    }

    static RenderType energyBubble(Identifier texture, float uOffset, float vOffset) {
        return RenderType.create("mcpj_bubble", RenderSetup.builder(BUBBLE)
                .withTexture("Sampler0", texture)
                .setTextureTransform(new TextureTransform.OffsetTextureTransform(uOffset, vOffset))
                .useLightmap()
                .sortOnUpload()
                .createRenderSetup());
    }
}
