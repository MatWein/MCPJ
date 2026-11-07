package de.mcpj.mc.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/** Minecraft 26.3's side of MCPJ's items: the tooltip, whose method differs between Minecraft versions. */
public abstract class ModItem extends Item {
    protected ModItem(Properties properties) {
        super(properties);
    }

    /** The lines below the item name. */
    protected abstract void addTooltip(Consumer<Component> tooltip);

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        addTooltip(tooltip);
    }
}
