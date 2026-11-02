package de.mcpj.client.input;

import de.mcpj.McpjMod;
import de.mcpj.client.PilotContext;
import de.mcpj.client.targeting.TargetSelector;
import de.mcpj.core.jumper.JumperAction;
import de.mcpj.entity.jumper.PuddleJumperEntity;
import de.mcpj.network.JumperActionPacket;
import de.mcpj.network.ModNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

@Mod.EventBusSubscriber(modid = McpjMod.MOD_ID, value = Dist.CLIENT)
public final class PilotInputHandler {
    private static final float TARGET_CLICK_VOLUME = 0.25f;
    private static final float TARGET_SELECTED_PITCH = 1.6f;
    private static final float TARGET_CLEARED_PITCH = 1.0f;

    private static final Map<KeyMapping, JumperAction> TOGGLES = Map.of(
            KeyBindings.CLOAK, JumperAction.CLOAK,
            KeyBindings.SHIELD, JumperAction.SHIELD,
            KeyBindings.PODS, JumperAction.PODS,
            KeyBindings.LIGHTS, JumperAction.LIGHTS);

    private PilotInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (PilotContext.isPiloting()) suppressShadowedVanillaKeys(Minecraft.getInstance().options);
            return;
        }
        KeyBindings.discardClicks(KeyBindings.TARGET);
        KeyBindings.discardClicks(KeyBindings.CARGO);
        KeyBindings.discardClicks(KeyBindings.STRAFE_LEFT);
        KeyBindings.discardClicks(KeyBindings.STRAFE_RIGHT);
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (jumper == null) {
            TargetSelector.clear();
            TOGGLES.keySet().forEach(KeyBindings::discardClicks);
            return;
        }
        TOGGLES.forEach((key, action) -> {
            while (key.consumeClick()) send(action, JumperActionPacket.NO_TARGET);
        });
        TargetSelector.validate(jumper);
    }

    private static void suppressShadowedVanillaKeys(Options options) {
        suppressIfShared(KeyBindings.STRAFE_LEFT, options.keyDrop);
        suppressIfShared(KeyBindings.STRAFE_RIGHT, options.keyInventory);
        suppressIfShared(KeyBindings.LIGHTS, options.keyAdvancements);
    }

    private static void suppressIfShared(KeyMapping ours, KeyMapping vanilla) {
        if (ours.getKey().equals(vanilla.getKey())) KeyBindings.discardClicks(vanilla);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null || minecraft.player == null) return;
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (jumper == null) return;
        if (KeyBindings.matches(KeyBindings.CARGO, event.getKey())) {
            minecraft.player.sendOpenInventory();
        } else if (KeyBindings.matches(KeyBindings.TARGET, event.getKey())) {
            TargetSelector.cycle(jumper);
            float pitch = TargetSelector.target() != null ? TARGET_SELECTED_PITCH : TARGET_CLEARED_PITCH;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), pitch, TARGET_CLICK_VOLUME));
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (jumper == null) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (FireControl.tryFire(jumper.level())) {
            Entity target = TargetSelector.target();
            send(JumperAction.FIRE, target != null ? target.getId() : JumperActionPacket.NO_TARGET);
        }
    }

    private static void send(JumperAction action, int targetId) {
        ModNetwork.sendToServer(new JumperActionPacket(action, targetId));
    }
}
