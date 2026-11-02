package de.mcpj.core.render;

/** Model space to view space (camera at the origin) of the current pose; needed for view-dependent effects. */
public interface ViewTransform {
    /** Writes the view-space position of the model point into {@code out} (x, y, z). */
    void position(float x, float y, float z, float[] out);

    /** Writes the view-space direction of the model normal into {@code out} (not necessarily normalized). */
    void normal(float x, float y, float z, float[] out);
}
