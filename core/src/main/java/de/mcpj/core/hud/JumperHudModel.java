package de.mcpj.core.hud;

import de.mcpj.core.config.ClientConfig;
import de.mcpj.core.jumper.Jumper;
import de.mcpj.core.jumper.JumperEnergy;
import de.mcpj.core.targeting.TargetKind;
import de.mcpj.core.util.Units;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** What the cockpit HUD shows (lines, values, warnings, colors); the platform only draws it. */
public final class JumperHudModel {
    private static final float HULL_GOOD = 0.6f;
    private static final float HULL_WARNING = 0.3f;
    private static final float ENERGY_GOOD = 0.3f;
    private static final int PERCENT = 100;
    private static final int LOW_ENERGY_ALARM_INTERVAL = 40;
    private static final String KEY = "hud.mcpj.";

    /** The selected target as the HUD shows it. */
    public record Target(Object displayName, TargetKind kind) {}

    private JumperHudModel() {}

    public static List<HudLine> lines(Jumper jumper, int tickCount, double altitude, boolean dronesReady, @Nullable Target target) {
        List<HudLine> lines = new ArrayList<>();
        lines.add(HudLine.of(text("title"), HudColors.GOLD));
        lines.add(HudLine.of(text("speed", HudText.literal(HudFormat.oneDecimal(Units.perSecond(jumper.animation().speed())))), HudColors.TEAL));
        lines.add(HudLine.of(text("altitude", HudText.literal((int) Math.floor(altitude))), HudColors.TEAL));
        lines.add(hullLine(jumper, tickCount));
        lines.add(energyLine(jumper, tickCount));
        lines.add(toggle("cloak", jumper.isCloaked(), HudColors.GOLD));
        lines.add(toggle("shield", jumper.isShielded(), HudColors.TEAL));
        lines.add(toggle("pods", jumper.animation().podsMostlyOut(), HudColors.TEAL));
        lines.add(toggle("lights", jumper.isLightsOn(), HudColors.GOLD));
        lines.add(droneLine(dronesReady));
        lines.add(targetLine(target));
        return lines;
    }

    /** Whether the low energy alarm beeps in this tick. */
    public static boolean lowEnergyAlarm(Jumper jumper, int tickCount) {
        return tickCount % LOW_ENERGY_ALARM_INTERVAL == 0 && jumper.energy().fraction() < ClientConfig.lowEnergyWarning();
    }

    private static HudLine hullLine(Jumper jumper, int tickCount) {
        float hull = jumper.hull().integrity();
        int color = hull > HULL_GOOD ? HudColors.TEAL : hull > HULL_WARNING ? HudColors.GOLD : HudColors.RED;
        List<HudText> parts = new ArrayList<>();
        parts.add(text("hull", HudText.literal(Math.round(hull * PERCENT))));
        if (jumper.touchesLava()) {
            parts.add(text("lava_warning"));
            color = HudColors.blink(tickCount, HudColors.RED, color);
        } else if (jumper.isSubmerged() && !jumper.isShielded()) {
            parts.add(text("water_warning"));
            color = HudColors.blink(tickCount, HudColors.RED, color);
        }
        return new HudLine(parts, color);
    }

    private static HudLine energyLine(Jumper jumper, int tickCount) {
        JumperEnergy energy = jumper.energy();
        int flow = energy.flow();
        List<HudText> parts = new ArrayList<>();
        parts.add(text("energy", HudText.literal((int) Math.floor(energy.fraction() * PERCENT))));
        if (flow > 0) {
            parts.add(HudText.literal(" · -" + HudFormat.compact(flow) + " FE/t · ~" + HudFormat.duration(energy.stored() / flow)));
        } else if (flow < 0) {
            parts.add(HudText.literal(" · +" + HudFormat.compact(-flow) + " FE/t"));
        }
        return new HudLine(parts, energyColor(jumper, tickCount));
    }

    private static int energyColor(Jumper jumper, int tickCount) {
        float fraction = jumper.energy().fraction();
        if (fraction > ENERGY_GOOD) return jumper.energy().flow() < 0 ? HudColors.GREEN : HudColors.TEAL;
        if (fraction > ClientConfig.lowEnergyWarning()) return HudColors.GOLD;
        return HudColors.blink(tickCount, HudColors.RED, HudColors.DARK_RED);
    }

    private static HudLine toggle(String system, boolean on, int activeColor) {
        HudText state = text((on ? "on." : "off.") + system);
        return HudLine.of(text(system, state), on ? activeColor : HudColors.GREY);
    }

    private static HudLine droneLine(boolean ready) {
        return HudLine.of(text("drones", text(ready ? "ready" : "loading")), ready ? HudColors.GOLD : HudColors.GREY);
    }

    private static HudLine targetLine(@Nullable Target target) {
        HudText name = target != null ? new HudText.Name(target.displayName()) : text("no_target");
        int color = target != null ? HudColors.rgb(HudColors.marker(target.kind(), true)) : HudColors.GREY;
        return HudLine.of(text("target", name), color);
    }

    private static HudText text(String key, HudText... args) {
        return HudText.translated(KEY + key, args);
    }
}
