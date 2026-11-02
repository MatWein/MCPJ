package de.mcpj.client.hud;

import de.mcpj.client.PilotContext;
import de.mcpj.client.input.FireControl;
import de.mcpj.client.targeting.TargetSelector;
import de.mcpj.core.config.ClientConfig;
import de.mcpj.entity.jumper.JumperEnergy;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import de.mcpj.core.util.Units;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.ArrayList;
import java.util.List;

public final class JumperHud {
    private static final int MARGIN_LEFT = 8;
    private static final int MARGIN_BOTTOM = 12;
    private static final int LINE_HEIGHT = 11;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_WIDTH = 200;
    private static final int ACCENT_WIDTH = 2;
    private static final float HULL_GOOD = 0.6f;
    private static final float HULL_WARNING = 0.3f;
    private static final float ENERGY_GOOD = 0.3f;
    private static final int PERCENT = 100;
    private static final String KEY = "hud.mcpj.";

    private record Line(Component text, int color) {}

    private JumperHud() {}

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (minecraft.options.hideGui || jumper == null) return;
        List<Line> lines = lines(jumper);
        int top = height - MARGIN_BOTTOM - lines.size() * LINE_HEIGHT;
        int bottom = top + lines.size() * LINE_HEIGHT;
        graphics.fill(MARGIN_LEFT - PANEL_PADDING, top - PANEL_PADDING, MARGIN_LEFT + PANEL_WIDTH, bottom, HudColors.PANEL_BACKGROUND);
        graphics.fill(MARGIN_LEFT - PANEL_PADDING, top - PANEL_PADDING, MARGIN_LEFT - PANEL_PADDING + ACCENT_WIDTH, bottom, HudColors.PANEL_ACCENT);
        Font font = minecraft.font;
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i).text, MARGIN_LEFT, top + i * LINE_HEIGHT, lines.get(i).color, true);
        }
    }

    private static List<Line> lines(PuddleJumperEntity jumper) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.translatable(KEY + "title"), HudColors.GOLD));
        lines.add(new Line(Component.translatable(KEY + "speed", HudFormat.oneDecimal(Units.perSecond(jumper.animation().speed()))), HudColors.TEAL));
        lines.add(new Line(Component.translatable(KEY + "altitude", Mth.floor(jumper.getY())), HudColors.TEAL));
        lines.add(hullLine(jumper));
        lines.add(energyLine(jumper));
        lines.add(toggle("cloak", jumper.isCloaked(), HudColors.GOLD));
        lines.add(toggle("shield", jumper.isShielded(), HudColors.TEAL));
        lines.add(toggle("pods", jumper.animation().podsMostlyOut(), HudColors.TEAL));
        lines.add(toggle("lights", jumper.isLightsOn(), HudColors.GOLD));
        lines.add(droneLine(jumper));
        lines.add(targetLine());
        return lines;
    }

    private static Line hullLine(PuddleJumperEntity jumper) {
        float hull = jumper.hull().integrity();
        int color = hull > HULL_GOOD ? HudColors.TEAL : hull > HULL_WARNING ? HudColors.GOLD : HudColors.RED;
        MutableComponent text = Component.translatable(KEY + "hull", Math.round(hull * PERCENT));
        if (jumper.touchesLava()) {
            text.append(Component.translatable(KEY + "lava_warning"));
            color = HudColors.blink(jumper.tickCount, HudColors.RED, color);
        } else if (jumper.isSubmerged() && !jumper.isShielded()) {
            text.append(Component.translatable(KEY + "water_warning"));
            color = HudColors.blink(jumper.tickCount, HudColors.RED, color);
        }
        return new Line(text, color);
    }

    private static Line energyLine(PuddleJumperEntity jumper) {
        JumperEnergy energy = jumper.energy();
        int flow = energy.flow();
        MutableComponent text = Component.translatable(KEY + "energy", (int) Math.floor(energy.fraction() * PERCENT));
        if (flow > 0) {
            text.append(" · -" + HudFormat.compact(flow) + " FE/t · ~" + HudFormat.duration(energy.stored() / flow));
        } else if (flow < 0) {
            text.append(" · +" + HudFormat.compact(-flow) + " FE/t");
        }
        return new Line(text, energyColor(jumper));
    }

    private static int energyColor(PuddleJumperEntity jumper) {
        float fraction = jumper.energy().fraction();
        if (fraction > ENERGY_GOOD) return jumper.energy().flow() < 0 ? HudColors.GREEN : HudColors.TEAL;
        if (fraction > ClientConfig.lowEnergyWarning()) return HudColors.GOLD;
        return HudColors.blink(jumper.tickCount, HudColors.RED, HudColors.DARK_RED);
    }

    private static Line toggle(String system, boolean on, int activeColor) {
        Component state = Component.translatable(KEY + (on ? "on." : "off.") + system);
        return new Line(Component.translatable(KEY + system, state), on ? activeColor : HudColors.GREY);
    }

    private static Line droneLine(PuddleJumperEntity jumper) {
        boolean ready = FireControl.isReady(jumper.level());
        Component state = Component.translatable(KEY + (ready ? "ready" : "loading"));
        return new Line(Component.translatable(KEY + "drones", state), ready ? HudColors.GOLD : HudColors.GREY);
    }

    private static Line targetLine() {
        Entity target = TargetSelector.target();
        Component name = target != null ? target.getDisplayName() : Component.translatable(KEY + "no_target");
        int color = target != null ? HudColors.rgb(HudColors.marker(target, true)) : HudColors.GREY;
        return new Line(Component.translatable(KEY + "target", name), color);
    }
}
