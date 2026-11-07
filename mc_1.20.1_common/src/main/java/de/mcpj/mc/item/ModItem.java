package de.mcpj.mc.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;

/** Minecraft 1.20.1's side of MCPJ's items: the tooltip, whose method differs between Minecraft versions. */
public abstract class ModItem extends Item {
    protected ModItem(Properties properties) {
        super(properties);
    }

    /** The lines below the item name. */
    protected abstract void addTooltip(Consumer<Component> tooltip);

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(tooltip::add);
    }
}
