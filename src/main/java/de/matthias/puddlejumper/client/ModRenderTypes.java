package de.matthias.puddlejumper.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Eigene Rendertypen (Unterklasse nur für den Zugriff auf die geschützten Render-Zustände). */
public final class ModRenderTypes extends RenderType {
    private ModRenderTypes(String name, VertexFormat fmt, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
                           Runnable setup, Runnable clear) {
        super(name, fmt, mode, size, crumbling, sort, setup, clear);
    }

    /**
     * Energieblase: wie der Creeper-Ladungseffekt (additiv, scrollende Textur), aber ohne Tiefenschreiben.
     * So verdeckt die Blase keine später gezeichneten durchsichtigen Blöcke (Eis, Wasser, Glas).
     */
    public static RenderType bubble(ResourceLocation texture, float u, float v) {
        return create("puddlejumper_bubble", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTexturingState(new OffsetTexturingStateShard(u, v))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false));
    }
}
