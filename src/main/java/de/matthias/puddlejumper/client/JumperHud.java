package de.matthias.puddlejumper.client;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Cockpit-Anzeige unten links: Geschwindigkeit, Höhe, Systeme, Ziel. */
public final class JumperHud {
    private static final int TEAL = 0x7FE6F2;
    private static final int GOLD = 0xFFC94A;
    private static final int GREY = 0x9AA8AD;

    private JumperHud() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        PuddleJumperEntity j = ClientState.pilotedJumper();
        if (j == null) return;
        Font font = mc.font;

        int x = 8;
        int lines = 9;
        int y = height - 12 - lines * 11;
        g.fill(x - 4, y - 4, x + 160, y + lines * 11, 0x80102028);
        g.fill(x - 4, y - 4, x - 2, y + lines * 11, 0xFF7FE6F2);

        double mps = j.clientSpeed * 20.0;
        line(g, font, x, y, Component.translatable("hud.puddlejumper.title"), GOLD);
        line(g, font, x, y += 11, Component.translatable("hud.puddlejumper.speed", String.format("%.1f", mps)), TEAL);
        line(g, font, x, y += 11, Component.translatable("hud.puddlejumper.altitude", Mth.floor(j.getY())), TEAL);
        float hull = j.getHealth() / PuddleJumperEntity.MAX_HEALTH;
        int hullColor = hull > 0.6f ? TEAL : hull > 0.3f ? GOLD : 0xFF5040;
        line(g, font, x, y += 11, Component.translatable("hud.puddlejumper.hull", Math.round(hull * 100)), hullColor);
        line(g, font, x, y += 11, state("cloak", j.isCloaked()), j.isCloaked() ? GOLD : GREY);
        line(g, font, x, y += 11, state("shield", j.isShielded()), j.isShielded() ? TEAL : GREY);
        line(g, font, x, y += 11, state("pods", j.podAnim > 0.5f), j.podAnim > 0.5f ? TEAL : GREY);

        long since = j.level().getGameTime() - ClientState.lastFireTick;
        boolean ready = since >= PuddleJumperEntity.DRONE_COOLDOWN;
        Component drone = Component.translatable("hud.puddlejumper.drones",
                Component.translatable(ready ? "hud.puddlejumper.ready" : "hud.puddlejumper.loading"));
        line(g, font, x, y += 11, drone, ready ? GOLD : GREY);
        Component target = ClientState.target != null ? ClientState.target.getDisplayName()
                : Component.translatable("hud.puddlejumper.no_target");
        int targetColor = ClientState.target != null ? TargetMarkers.colorFor(ClientState.target, true) & 0xFFFFFF : GREY;
        line(g, font, x, y += 11, Component.translatable("hud.puddlejumper.target", target), targetColor);
    }

    private static Component state(String key, boolean on) {
        return Component.translatable("hud.puddlejumper." + key,
                Component.translatable(on ? "hud.puddlejumper.on." + key : "hud.puddlejumper.off." + key));
    }

    private static void line(GuiGraphics g, Font font, int x, int y, Component text, int color) {
        g.drawString(font, text, x, y, color, true);
    }
}
