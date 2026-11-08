package de.mcpj.mc.client.targeting;

import de.mcpj.core.hud.HudColors;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class TargetMarkers {
    private record Marker(Entity entity, float minX, float minY, float maxX, float maxY) {}

    private static final List<Marker> MARKERS = new ArrayList<>();
    private static final double BOX_MARGIN = 0.1;
    private static final float MIN_CLIP_W = 0.05f;
    private static final int MIN_SIZE = 6;
    private static final int MIN_BRACKET = 2;
    private static final int BRACKET = 4;
    private static final int BRACKET_SELECTED = 8;
    private static final int BRACKET_DIVISOR = 3;
    private static final int OUTER_OFFSET = 2;
    private static final int LABEL_OFFSET = 12;

    private TargetMarkers() {}

    /**
     * World rendering (after translucent blocks): projects the targets to screen boxes for {@link #render}.
     * {@code view} is the camera rotation: the top of the pose stack up to Minecraft 1.20.1, a matrix of its own since
     * 1.21.
     */
    public static void collect(Matrix4f view, Matrix4f projection, Camera camera, float partialTick) {
        MARKERS.clear();
        List<Entity> entities = MarkedEntities.current();
        if (entities.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        Matrix4f viewProjection = new Matrix4f(projection).mul(view);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Vec3 cameraPosition = camera.getPosition();

        for (Entity entity : entities) {
            Marker marker = project(entity, interpolatedBox(entity, partialTick, cameraPosition), viewProjection, width, height);
            if (marker != null) MARKERS.add(marker);
        }
    }

    private static AABB interpolatedBox(Entity entity, float partialTick, Vec3 camera) {
        double dx = Mth.lerp(partialTick, entity.xo, entity.getX()) - entity.getX();
        double dy = Mth.lerp(partialTick, entity.yo, entity.getY()) - entity.getY();
        double dz = Mth.lerp(partialTick, entity.zo, entity.getZ()) - entity.getZ();
        AABB box = entity instanceof PuddleJumperEntity jumper ? jumper.fullBoundingBox() : entity.getBoundingBox();
        return box.move(dx - camera.x, dy - camera.y, dz - camera.z).inflate(BOX_MARGIN);
    }

    private static Marker project(Entity entity, AABB box, Matrix4f viewProjection, int width, int height) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        Vector4f corner = new Vector4f();
        for (int i = 0; i < 8; i++) {
            corner.set((float) ((i & 1) == 0 ? box.minX : box.maxX), (float) ((i & 2) == 0 ? box.minY : box.maxY),
                    (float) ((i & 4) == 0 ? box.minZ : box.maxZ), 1f);
            viewProjection.transform(corner);
            if (corner.w <= MIN_CLIP_W) return null;
            float screenX = (corner.x / corner.w * 0.5f + 0.5f) * width;
            float screenY = (0.5f - corner.y / corner.w * 0.5f) * height;
            minX = Math.min(minX, screenX);
            maxX = Math.max(maxX, screenX);
            minY = Math.min(minY, screenY);
            maxY = Math.max(maxY, screenY);
        }
        boolean onScreen = maxX >= 0 && maxY >= 0 && minX <= width && minY <= height;
        return onScreen ? new Marker(entity, minX, minY, maxX, maxY) : null;
    }

    /** HUD: draws the markers collected in this frame. */
    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        for (Marker marker : MARKERS) drawMarker(graphics, minecraft, marker);
    }

    private static void drawMarker(GuiGraphics graphics, Minecraft minecraft, Marker marker) {
        boolean selected = marker.entity == TargetSelector.target();
        int color = HudColors.marker(TargetSelector.kind(marker.entity), selected);
        int x0 = Math.round(marker.minX), y0 = Math.round(marker.minY), x1 = Math.round(marker.maxX), y1 = Math.round(marker.maxY);
        if (x1 - x0 < MIN_SIZE) { x0 -= MIN_SIZE / 2; x1 += MIN_SIZE / 2; }
        if (y1 - y0 < MIN_SIZE) { y0 -= MIN_SIZE / 2; y1 += MIN_SIZE / 2; }
        int length = Math.max(MIN_BRACKET, Math.min(selected ? BRACKET_SELECTED : BRACKET, Math.min(x1 - x0, y1 - y0) / BRACKET_DIVISOR));
        brackets(graphics, x0, y0, x1, y1, length, color);
        if (!selected) return;
        brackets(graphics, x0 - OUTER_OFFSET, y0 - OUTER_OFFSET, x1 + OUTER_OFFSET, y1 + OUTER_OFFSET, length, HudColors.halfTransparent(color));
        Font font = minecraft.font;
        double distance = Math.sqrt(TargetSelector.distanceSqr(marker.entity, minecraft.player.position()));
        Component label = Component.literal(marker.entity.getDisplayName().getString() + "  " + Math.round(distance) + " m");
        graphics.drawString(font, label, (x0 + x1 - font.width(label)) / 2, y0 - LABEL_OFFSET, color, true);
    }

    private static void brackets(GuiGraphics graphics, int x0, int y0, int x1, int y1, int length, int color) {
        graphics.fill(x0, y0, x0 + length, y0 + 1, color);
        graphics.fill(x0, y0, x0 + 1, y0 + length, color);
        graphics.fill(x1 - length, y0, x1, y0 + 1, color);
        graphics.fill(x1 - 1, y0, x1, y0 + length, color);
        graphics.fill(x0, y1 - 1, x0 + length, y1, color);
        graphics.fill(x0, y1 - length, x0 + 1, y1, color);
        graphics.fill(x1 - length, y1 - 1, x1, y1, color);
        graphics.fill(x1 - 1, y1 - length, x1, y1, color);
    }
}
