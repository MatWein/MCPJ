package de.mcpj.core.render;

/**
 * Receives vertices in model space (four per quad); the platform transforms them with its current pose and writes
 * them into its vertex buffers (with its own light and overlay values).
 */
@FunctionalInterface
public interface VertexSink {
    void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v,
                float normalX, float normalY, float normalZ);
}
