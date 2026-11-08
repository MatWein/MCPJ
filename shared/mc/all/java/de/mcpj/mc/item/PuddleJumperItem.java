package de.mcpj.mc.item;

import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import de.mcpj.mc.compat.McCompat;

import java.util.function.Consumer;

public class PuddleJumperItem extends ModItem {
    public PuddleJumperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 spawn = Vec3.atBottomCenterOf(pos);
        PuddleJumperEntity jumper = McCompat.newJumper(level, spawn.x, spawn.y, spawn.z, player != null ? player.getYRot() : 0f);
        if (jumper == null) return InteractionResult.FAIL;
        if (!level.noCollision(jumper, jumper.getBoundingBox())) {
            if (player != null && level.isClientSide()) {
                McCompat.overlayMessage(player, Component.translatable("message.mcpj.no_space"));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.addFreshEntity(jumper);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return McCompat.success(level);
    }

    @Override
    protected void addTooltip(ItemStack stack, Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("item.mcpj.puddle_jumper.desc").withStyle(ChatFormatting.GRAY));
    }
}
