package de.mcpj.core.projector;

/** What the projector logic needs from the block in the world and the jumper it builds. */
public interface ProjectorHost {
    /** No blocks (and no other ship) where the jumper stands above the projector. */
    boolean isSpaceFree();

    /** Places the jumper under construction; false if that is not possible. */
    boolean startJumper();

    /** The jumper under construction exists (loaded and alive). */
    boolean hasJumper();

    /** Shows the build progress (0..1) on the jumper. */
    void updateJumper(float progress);

    /** The build is complete: the jumper becomes a normal ship. */
    void finishJumper();

    /** Building (glowing) or idle. */
    void setBuilding(boolean building);

    void markChanged();
}
