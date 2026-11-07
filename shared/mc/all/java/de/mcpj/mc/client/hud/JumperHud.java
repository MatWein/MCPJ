package de.mcpj.mc.client.hud;

import de.mcpj.mc.client.ClientCompat;
import de.mcpj.mc.client.PilotContext;
import de.mcpj.mc.client.input.FireControl;
import de.mcpj.mc.client.targeting.TargetSelector;
import de.mcpj.core.hud.HudColors;
import de.mcpj.core.hud.HudLine;
import de.mcpj.core.hud.HudText;
import de.mcpj.core.hud.JumperHudModel;
import de.mcpj.core.math.Vec3d;
import de.mcpj.core.navigation.Compass;
import de.mcpj.core.navigation.Waypoint;
import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.util.Convert;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;
import java.util.List;

/** Draws the core's cockpit HUD lines ({@link JumperHudModel}) bottom left and the compass band top center. */
public final class JumperHud {
    private static final int MARGIN_LEFT = 8;
    private static final int MARGIN_BOTTOM = 12;
    private static final int LINE_HEIGHT = 11;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_WIDTH = 200;
    private static final int ACCENT_WIDTH = 2;
    private static final int COMPASS_TOP = 4;
    private static final int COMPASS_HALF_WIDTH = 100;
    private static final int COMPASS_HEIGHT = 14;
    private static final int COMPASS_PADDING = 6;
    /** Degrees shown left and right of the heading. */
    private static final float COMPASS_HALF_RANGE = 75f;
    private static final int TICK_TOP = 9;
    private static final int LABEL_TOP = 3;
    private static final int MARKER_SIZE = 3;
    private static final int NAVIGATION_LINE_GAP = 6;
    private static final int OPAQUE = 0xFF000000;
    private static final int TICK_COLOR = 0xB0000000 | HudColors.TEAL;

    private JumperHud() {}

    public static void render(HudCanvas canvas, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (ClientCompat.hudHidden(minecraft) || jumper == null) return;
        Entity selected = TargetSelector.target();
        JumperHudModel.Target target = selected != null
                ? new JumperHudModel.Target(selected.getDisplayName(), TargetSelector.kind(selected)) : null;
        List<HudLine> lines = JumperHudModel.lines(jumper.core(), jumper.tickCount, jumper.getY(), FireControl.isReady(jumper.level()), target);
        int top = height - MARGIN_BOTTOM - lines.size() * LINE_HEIGHT;
        int bottom = top + lines.size() * LINE_HEIGHT;
        canvas.fill(MARGIN_LEFT - PANEL_PADDING, top - PANEL_PADDING, MARGIN_LEFT + PANEL_WIDTH, bottom, HudColors.PANEL_BACKGROUND);
        canvas.fill(MARGIN_LEFT - PANEL_PADDING, top - PANEL_PADDING, MARGIN_LEFT - PANEL_PADDING + ACCENT_WIDTH, bottom, HudColors.PANEL_ACCENT);
        Font font = minecraft.font;
        for (int i = 0; i < lines.size(); i++) {
            HudLine line = lines.get(i);
            canvas.text(font, component(line), MARGIN_LEFT, top + i * LINE_HEIGHT, line.color());
        }
        renderCompass(canvas, font, width, jumper);
    }

    /** Compass band with direction marks, the destination marker and the navigation line below. */
    private static void renderCompass(HudCanvas canvas, Font font, int width, PuddleJumperEntity jumper) {
        int center = width / 2;
        int left = center - COMPASS_HALF_WIDTH - COMPASS_PADDING;
        int right = center + COMPASS_HALF_WIDTH + COMPASS_PADDING;
        int bottom = COMPASS_TOP + COMPASS_HEIGHT;
        canvas.fill(left, COMPASS_TOP, right, bottom, HudColors.PANEL_BACKGROUND);
        canvas.fill(left, bottom, right, bottom + 1, HudColors.PANEL_ACCENT);
        float heading = Compass.heading(jumper.getYRot());
        for (Compass.Mark mark : Compass.marks(heading, COMPASS_HALF_RANGE)) {
            int x = center + Math.round(mark.offset() / COMPASS_HALF_RANGE * COMPASS_HALF_WIDTH);
            if (mark.direction() == null) {
                canvas.fill(x, COMPASS_TOP + TICK_TOP, x + 1, bottom - 1, TICK_COLOR);
            } else {
                Component label = Component.translatable("hud.mcpj.compass." + mark.direction());
                int color = mark.direction().equals("n") ? HudColors.RED : HudColors.TEAL;
                canvas.text(font, label, x - font.width(label) / 2 + 1, COMPASS_TOP + LABEL_TOP, color);
            }
        }
        // the ship's heading
        canvas.fill(center, bottom - MARKER_SIZE, center + 1, bottom + MARKER_SIZE, OPAQUE | HudColors.GOLD);

        Waypoint destination = jumper.core().navigation().destination();
        if (destination == null) return;
        Vec3d position = Convert.toCore(jumper.position());
        boolean sameDimension = destination.dimension().equals(McCompat.dimensionId(jumper.level()));
        if (sameDimension) {
            float relative = Compass.relative(Compass.bearing(position, destination.center()), heading);
            float shown = Math.max(-COMPASS_HALF_RANGE, Math.min(COMPASS_HALF_RANGE, relative));
            int x = center + Math.round(shown / COMPASS_HALF_RANGE * COMPASS_HALF_WIDTH);
            drawMarker(canvas, x, bottom + 1);
            if (Math.abs(relative) > COMPASS_HALF_RANGE) {
                Component arrow = Component.literal(relative < 0 ? "◀" : "▶");
                int arrowX = relative < 0 ? left - font.width(arrow) - 2 : right + 2;
                canvas.text(font, arrow, arrowX, COMPASS_TOP + LABEL_TOP, HudColors.GOLD);
            }
        }
        HudLine line = JumperHudModel.navigationLine(destination, position, sameDimension, jumper.core().animation().speed());
        Component text = component(line);
        canvas.text(font, text, center - font.width(text) / 2, bottom + NAVIGATION_LINE_GAP, line.color());
    }

    /** A small triangle pointing up at the band. */
    private static void drawMarker(HudCanvas canvas, int x, int top) {
        for (int row = 0; row < MARKER_SIZE; row++) {
            canvas.fill(x - row, top + row, x + row + 1, top + row + 1, OPAQUE | HudColors.GOLD);
        }
    }

    private static Component component(HudLine line) {
        MutableComponent text = Component.empty();
        line.parts().forEach(part -> text.append(component(part)));
        return text;
    }

    private static Component component(HudText text) {
        if (text instanceof HudText.Translated translated) {
            return Component.translatable(translated.key(), Arrays.stream(translated.args()).map(JumperHud::component).toArray());
        }
        if (text instanceof HudText.Name name && name.source() instanceof Component component) return component;
        if (text instanceof HudText.Name name) return Component.literal(String.valueOf(name.source()));
        return Component.literal(((HudText.Literal) text).text());
    }
}
