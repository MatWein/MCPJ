package de.mcpj.mc.client.input;

import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.client.PilotContext;
import de.mcpj.mc.client.targeting.TargetSelector;
import de.mcpj.core.jumper.JumperAction;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.network.JumperActionPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public final class PilotInputHandler {
    private static final float TARGET_CLICK_VOLUME = 0.25f;
    private static final float TARGET_SELECTED_PITCH = 1.6f;
    private static final float TARGET_CLEARED_PITCH = 1.0f;

    private static final Map<KeyMapping, JumperAction> TOGGLES = Map.of(
            KeyBindings.CLOAK, JumperAction.CLOAK,
            KeyBindings.SHIELD, JumperAction.SHIELD,
            KeyBindings.PODS, JumperAction.PODS);

    private PilotInputHandler() {}

    /** Start of every client tick: vanilla keys sharing a key with ours do nothing while piloting. */
    public static void onClientTickStart() {
        if (PilotContext.isPiloting()) suppressShadowedVanillaKeys(Minecraft.getInstance().options);
    }

    /** End of every client tick: toggles and target validation. */
    public static void onClientTickEnd() {
        KeyBindings.discardClicks(KeyBindings.TARGET);
        KeyBindings.discardClicks(KeyBindings.CARGO);
        KeyBindings.discardClicks(KeyBindings.LIGHTS);
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
        if (KeyBindings.boundKey(ours).equals(KeyBindings.boundKey(vanilla))) KeyBindings.discardClicks(vanilla);
    }

    /** A raw key event (GLFW key and action). */
    public static void onKey(int key, int action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (action != GLFW.GLFW_PRESS || minecraft.screen != null || minecraft.player == null) return;
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (jumper == null) return;
        // keys that may share a key with vanilla are read as raw key presses, so they work the same on every loader
        if (KeyBindings.matches(KeyBindings.CARGO, key)) {
            minecraft.player.sendOpenInventory();
        } else if (KeyBindings.matches(KeyBindings.LIGHTS, key)) {
            send(JumperAction.LIGHTS, JumperActionPacket.NO_TARGET);
        } else if (KeyBindings.matches(KeyBindings.TARGET, key)) {
            TargetSelector.cycle(jumper);
            float pitch = TargetSelector.target() != null ? TARGET_SELECTED_PITCH : TARGET_CLEARED_PITCH;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, TARGET_CLICK_VOLUME));
        }
    }

    /**
     * The attack key was pressed: while piloting it launches a drone instead.
     *
     * @return true if it was used here (the loader cancels the vanilla attack and the hand swing)
     */
    public static boolean onAttack() {
        PuddleJumperEntity jumper = PilotContext.pilotedJumper();
        if (jumper == null) return false;
        if (FireControl.tryFire(jumper.level())) {
            Entity target = TargetSelector.target();
            send(JumperAction.FIRE, target != null ? target.getId() : JumperActionPacket.NO_TARGET);
        }
        return true;
    }

    private static void send(JumperAction action, int targetId) {
        McpjPlatform.get().sendToServer(new JumperActionPacket(action, targetId));
    }
}
