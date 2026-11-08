package de.mcpj.mc.item;

import de.mcpj.mc.compat.McCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The key to one Puddle Jumper: blank until it is first used on a ship, then it stores that ship's id. The use itself
 * is handled by the ship (a jumper is no living entity, so Minecraft does not ask the item).
 */
public class JumperKeyItem extends ModItem {
    private static final String JUMPER_TAG = "mcpj_jumper";

    public JumperKeyItem(Properties properties) {
        super(properties);
    }

    public static boolean isKey(ItemStack stack) {
        return stack.getItem() instanceof JumperKeyItem;
    }

    /** The ship this stack is the key to; null if it is no key or still blank. */
    @Nullable
    public static UUID pairedJumper(ItemStack stack) {
        if (!isKey(stack)) return null;
        String id = McCompat.itemText(stack, JUMPER_TAG);
        if (id.isEmpty()) return null;
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            // edited by hand (e.g. /give): a key to nothing, read every frame by the HUD
            return null;
        }
    }

    public static void pair(ItemStack key, UUID jumper) {
        McCompat.setItemText(key, JUMPER_TAG, jumper.toString());
    }

    /** The ships the player carries a key to, anywhere in the inventory. */
    public static Set<UUID> pairedJumpers(Player player) {
        Set<UUID> jumpers = new HashSet<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            UUID jumper = pairedJumper(inventory.getItem(slot));
            if (jumper != null) jumpers.add(jumper);
        }
        return jumpers;
    }

    @Override
    protected void addTooltip(ItemStack stack, Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("item.mcpj.jumper_key.desc").withStyle(ChatFormatting.GRAY));
        String state = pairedJumper(stack) != null ? "item.mcpj.jumper_key.paired" : "item.mcpj.jumper_key.unpaired";
        tooltip.accept(Component.translatable(state).withStyle(ChatFormatting.GOLD));
    }
}
