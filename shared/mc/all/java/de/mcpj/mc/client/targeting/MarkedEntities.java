package de.mcpj.mc.client.targeting;

import de.mcpj.mc.client.PilotContext;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.item.JumperKeyItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** What the HUD marks: the targets around the piloted ship and, on foot too, the ships the player has a key to. */
final class MarkedEntities {
    private MarkedEntities() {}

    static List<Entity> current() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return List.of();
        List<Entity> marked = new ArrayList<>();
        PuddleJumperEntity piloted = PilotContext.pilotedJumper();
        if (piloted != null) {
            marked.addAll(TargetSelector.candidates(piloted));
            Entity selected = TargetSelector.target();
            if (selected != null && !marked.contains(selected)) marked.add(selected);
        }
        Set<UUID> keyed = JumperKeyItem.pairedJumpers(player);
        if (keyed.isEmpty()) return marked;
        for (Entity entity : level.entitiesForRendering()) {
            // also while cloaked: the key finds the ship anyway
            if (entity instanceof PuddleJumperEntity jumper && keyed.contains(jumper.getUUID())
                    && player.getVehicle() != jumper && !marked.contains(jumper)) {
                marked.add(jumper);
            }
        }
        return marked;
    }
}
