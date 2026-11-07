package de.mcpj.mc.item;

import java.util.List;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;


public class PuddleJumperItem extends Item {
    public PuddleJumperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        PuddleJumperEntity jumper = McpjPlatform.get().jumperType().create(level);
        if (jumper == null) return InteractionResult.FAIL;

        Vec3 spawn = Vec3.atBottomCenterOf(pos);
        jumper.moveTo(spawn.x, spawn.y, spawn.z, player != null ? player.getYRot() : 0f, 0f);
        if (!level.noCollision(jumper, jumper.getBoundingBox())) {
            if (player != null && level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.mcpj.no_space"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.addFreshEntity(jumper);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mcpj.puddle_jumper.desc").withStyle(ChatFormatting.GRAY));
    }
}
