#version 330
#extension GL_ARB_separate_shader_objects : require

// Fragment shader for MCPJ's additive render types (glow, shield bubble), used with vanilla's core/entity vertex
// shader (defines EMISSIVE, NO_OVERLAY, NO_CARDINAL_LIGHTING). Vanilla mixes fog towards the fog color, which an
// additive blend adds on top (bright blue under water); like Minecraft up to 1.21, fog fades the color out instead.

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 6) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif
    color *= vertexColor * ColorModulator;
    float fog = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart,
            FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    fragColor = vec4(color.rgb * (1.0 - fog), color.a);
}
