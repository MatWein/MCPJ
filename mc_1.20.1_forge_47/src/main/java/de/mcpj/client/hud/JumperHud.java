package de.mcpj.client.hud;

import de.mcpj.client.PilotContext;
import de.mcpj.client.input.FireControl;
import de.mcpj.client.targeting.TargetSelector;
import de.mcpj.core.hud.HudColors;
import de.mcpj.core.hud.HudLine;
import de.mcpj.core.hud.HudText;
import de.mcpj.core.hud.JumperHudModel;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.Arrays;
import java.util.List;

/** Draws the core's cockpit HUD lines ({@link JumperHudModel}) bottom left. */
public final class JumperHud {
    private static final int MARGIN_LEFT = 8;
    private static final int MARGIN_BOTTOM = 12;
    private static final int LINE_HEIGHT = 11;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_WIDTH = 200;
    private static final int ACCENT_WIDTH = 2;

    private JumperHud() {}

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (minecraft.options.hideGui || jumper == null) return;
        Entity selected = TargetSelector.target();
        JumperHudModel.Target target = selected != null
                ? new JumperHudModel.Target(selected.getDisplayName(), TargetSelector.kind(selected)) : null;
        List<HudLine> lines = JumperHudModel.lines(jumper.core(), jumper.tickCount, jumper.getY(), FireControl.isReady(jumper.level()), target);
        int top = height - MARGIN_BOTTOM - lines.size() * LINE_HEIGHT;
        int bottom = top + lines.size() * LINE_HEIGHT;
        graphics.fill(MARGIN_LEFT - PANEL_PADDING, top - PANEL_PADDING, MARGIN_LEFT + PANEL_WIDTH, bottom, HudColors.PANEL_BACKGROUND);
        graphics.fill(MARGIN_LEFT - PANEL_PADDING, top - PANEL_PADDING, MARGIN_LEFT - PANEL_PADDING + ACCENT_WIDTH, bottom, HudColors.PANEL_ACCENT);
        Font font = minecraft.font;
        for (int i = 0; i < lines.size(); i++) {
            HudLine line = lines.get(i);
            graphics.drawString(font, component(line), MARGIN_LEFT, top + i * LINE_HEIGHT, line.color(), true);
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
