package de.matthias.puddlejumper.client;

import de.matthias.puddlejumper.entity.PuddleJumperEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;

/**
 * Zielmarkierungen im Jumper: dezente Eck-Klammern um alle erfassbaren Ziele (Feinde rot, Jumper cyan,
 * sonst weiß), das mit Tab gewählte Ziel kräftig hervorgehoben. Die Bildschirm-Rechtecke werden beim Weltrendern projiziert und danach im HUD
 * gezeichnet.
 */
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class TargetMarkers {
    private record Marker(Entity entity, float x0, float y0, float x1, float y1) {}

    private static final List<Marker> MARKERS = new ArrayList<>();
    // dezent (nicht gewählt) / kräftig (gewählt)
    private static final int COLOR_NEUTRAL = 0x66FFFFFF, COLOR_NEUTRAL_SEL = 0xFFFFC94A;
    private static final int COLOR_ENEMY = 0x99FF3030, COLOR_ENEMY_SEL = 0xFFFF2A20;
    private static final int COLOR_JUMPER = 0x9960D8FF, COLOR_JUMPER_SEL = 0xFF60D8FF;

    private TargetMarkers() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        MARKERS.clear();
        PuddleJumperEntity jumper = ClientState.pilotedJumper();
        if (jumper == null) return;

        Minecraft mc = Minecraft.getInstance();
        float pt = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        Matrix4f viewProj = new Matrix4f(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();

        List<Entity> list = new ArrayList<>(ClientState.candidates(jumper));
        if (ClientState.target != null && !list.contains(ClientState.target)) list.add(ClientState.target);
        Vector4f v = new Vector4f();
        for (Entity e : list) {
            // interpolierte Hitbox relativ zur Kamera
            double ix = Mth.lerp(pt, e.xo, e.getX()) - e.getX();
            double iy = Mth.lerp(pt, e.yo, e.getY()) - e.getY();
            double iz = Mth.lerp(pt, e.zo, e.getZ()) - e.getZ();
            AABB base = e instanceof PuddleJumperEntity j ? j.fullBoundingBox() : e.getBoundingBox();
            AABB box = base.move(ix - cam.x, iy - cam.y, iz - cam.z).inflate(0.1);
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean visible = true;
            for (int c = 0; c < 8 && visible; c++) {
                v.set((float) ((c & 1) == 0 ? box.minX : box.maxX), (float) ((c & 2) == 0 ? box.minY : box.maxY),
                        (float) ((c & 4) == 0 ? box.minZ : box.maxZ), 1f);
                viewProj.transform(v);
                if (v.w <= 0.05f) {
                    visible = false; // (teilweise) hinter der Kamera
                    break;
                }
                float sx = (v.x / v.w * 0.5f + 0.5f) * w;
                float sy = (1f - (v.y / v.w * 0.5f + 0.5f)) * h;
                minX = Math.min(minX, sx); maxX = Math.max(maxX, sx);
                minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
            }
            if (!visible || maxX < 0 || maxY < 0 || minX > w || minY > h) continue;
            MARKERS.add(new Marker(e, minX, minY, maxX, maxY));
        }
    }

    /** HUD-Overlay: Klammern zeichnen. */
    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || ClientState.pilotedJumper() == null) return;
        Font font = mc.font;
        for (Marker m : MARKERS) {
            boolean selected = m.entity == ClientState.target;
            int color = colorFor(m.entity, selected);
            int x0 = Math.round(m.x0), y0 = Math.round(m.y0), x1 = Math.round(m.x1), y1 = Math.round(m.y1);
            if (x1 - x0 < 6) { x0 -= 3; x1 += 3; }
            if (y1 - y0 < 6) { y0 -= 3; y1 += 3; }
            int len = Math.max(2, Math.min(selected ? 8 : 4, Math.min(x1 - x0, y1 - y0) / 3));
            brackets(g, x0, y0, x1, y1, len, color);
            if (selected) {
                brackets(g, x0 - 2, y0 - 2, x1 + 2, y1 + 2, len, (color & 0x00FFFFFF) | 0x80000000);
                double dist = Math.sqrt(ClientState.distanceSq(m.entity, mc.player.position()));
                Component label = Component.literal(m.entity.getDisplayName().getString() + "  " + Math.round(dist) + " m");
                g.drawString(font, label, (x0 + x1 - font.width(label)) / 2, y0 - 12, color, true);
            }
        }
    }

    /** Feinde rot, andere Jumper cyan, sonst weiß (gewählt: golden). */
    public static int colorFor(Entity e, boolean selected) {
        if (e instanceof Enemy) return selected ? COLOR_ENEMY_SEL : COLOR_ENEMY;
        if (e instanceof PuddleJumperEntity) return selected ? COLOR_JUMPER_SEL : COLOR_JUMPER;
        return selected ? COLOR_NEUTRAL_SEL : COLOR_NEUTRAL;
    }

    private static void brackets(GuiGraphics g, int x0, int y0, int x1, int y1, int len, int c) {
        // oben links, oben rechts, unten links, unten rechts (je zwei 1px-Linien)
        g.fill(x0, y0, x0 + len, y0 + 1, c);
        g.fill(x0, y0, x0 + 1, y0 + len, c);
        g.fill(x1 - len, y0, x1, y0 + 1, c);
        g.fill(x1 - 1, y0, x1, y0 + len, c);
        g.fill(x0, y1 - 1, x0 + len, y1, c);
        g.fill(x0, y1 - len, x0 + 1, y1, c);
        g.fill(x1 - len, y1 - 1, x1, y1, c);
        g.fill(x1 - 1, y1 - len, x1, y1, c);
    }
}
