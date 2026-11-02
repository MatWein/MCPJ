package de.matthias.puddlejumper.client.render;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.client.PilotContext;
import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PuddleJumperMod.MOD_ID, value = Dist.CLIENT)
public final class PilotVisibility {
    private PilotVisibility() {}

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity().getVehicle() instanceof PuddleJumperEntity) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (PilotContext.isPiloting()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_LIST.id()) && PilotContext.isPiloting()) {
            event.setCanceled(true);
        }
    }
}
