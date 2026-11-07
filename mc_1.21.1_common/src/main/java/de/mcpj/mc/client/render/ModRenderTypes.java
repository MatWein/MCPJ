package de.mcpj.mc.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Subclass only to reach the protected render state shards. */
final class ModRenderTypes extends RenderType {
    private static final int BUFFER_SIZE = 256;

    private ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                           boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    /**
     * Like the charged creeper swirl (additive, scrolling texture) but without depth writes,
     * so the bubble does not hide translucent blocks rendered later (ice, water, glass).
     */
    static RenderType energyBubble(ResourceLocation texture, float uOffset, float vOffset) {
        return create("mcpj_bubble", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, BUFFER_SIZE, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTexturingState(new OffsetTexturingStateShard(uOffset, vOffset))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false));
    }
}
