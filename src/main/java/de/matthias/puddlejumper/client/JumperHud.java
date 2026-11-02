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
        int lines = 11;
        int y = height - 12 - lines * 11;
        g.fill(x - 4, y - 4, x + 200, y + lines * 11, 0x80102028);
        g.fill(x - 4, y - 4, x - 2, y + lines * 11, 0xFF7FE6F2);

        double mps = j.clientSpeed * 20.0;
        line(g, font, x, y, Component.translatable("hud.puddlejumper.title"), GOLD);
        line(g, font, x, y += 11, Component.translatable("hud.puddlejumper.speed", String.format("%.1f", mps)), TEAL);
        line(g, font, x, y += 11, Component.translatable("hud.puddlejumper.altitude", Mth.floor(j.getY())), TEAL);
        float hull = j.getHealth() / PuddleJumperEntity.MAX_HEALTH;
        int hullColor = hull > 0.6f ? TEAL : hull > 0.3f ? GOLD : 0xFF5040;
        Component hullText = Component.translatable("hud.puddlejumper.hull", Math.round(hull * 100));
        if (j.isSubmerged() && !j.isShielded()) {
            hullText = hullText.copy().append(Component.translatable("hud.puddlejumper.water_warning"));
            if ((j.tickCount / 10) % 2 == 0) hullColor = 0xFF5040; // blinkt
        }
        line(g, font, x, y += 11, hullText, hullColor);
        line(g, font, x, y += 11, energyText(j), energyColor(j));
        line(g, font, x, y += 11, state("cloak", j.isCloaked()), j.isCloaked() ? GOLD : GREY);
        line(g, font, x, y += 11, state("shield", j.isShielded()), j.isShielded() ? TEAL : GREY);
        line(g, font, x, y += 11, state("pods", j.podAnim > 0.5f), j.podAnim > 0.5f ? TEAL : GREY);
        line(g, font, x, y += 11, state("lights", j.isLightsOn()), j.isLightsOn() ? GOLD : GREY);

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

    /** "Energie: 62 % · −1,5k FE/t · ~12 min" bzw. "+50k FE/t" beim Laden. */
    private static Component energyText(PuddleJumperEntity j) {
        int pct = (int) Math.floor(j.getEnergyFraction() * 100);
        int flow = j.getEnergyFlow();
        Component c = Component.translatable("hud.puddlejumper.energy", pct);
        if (flow > 0) {
            long ticks = j.getEnergy() / Math.max(1, flow);
            c = c.copy().append(" · -" + compact(flow) + " FE/t · ~" + duration(ticks));
        } else if (flow < 0) {
            c = c.copy().append(" · +" + compact(-flow) + " FE/t");
        }
        return c;
    }

    private static int energyColor(PuddleJumperEntity j) {
        float f = j.getEnergyFraction();
        if (f > 0.3f) return j.getEnergyFlow() < 0 ? 0x7FF29A : TEAL;
        if (f > 0.1f) return GOLD;
        return (j.tickCount / 10) % 2 == 0 ? 0xFF5040 : 0x803020; // blinkt
    }

    private static String compact(int v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000f);
        if (v >= 1_000) return String.format("%.1fk", v / 1_000f);
        return Integer.toString(v);
    }

    private static String duration(long ticks) {
        long s = ticks / 20;
        if (s >= 3600) return (s / 3600) + " h " + (s % 3600) / 60 + " min";
        if (s >= 60) return (s / 60) + " min";
        return s + " s";
    }

    private static Component state(String key, boolean on) {
        return Component.translatable("hud.puddlejumper." + key,
                Component.translatable(on ? "hud.puddlejumper.on." + key : "hud.puddlejumper.off." + key));
    }

    private static void line(GuiGraphics g, Font font, int x, int y, Component text, int color) {
        g.drawString(font, text, x, y, color, true);
    }
}
