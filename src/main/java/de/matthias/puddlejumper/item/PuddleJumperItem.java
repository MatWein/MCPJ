package de.matthias.puddlejumper.item;

import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
import de.matthias.puddlejumper.registry.ModEntities;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PuddleJumperItem extends Item {
    public PuddleJumperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        PuddleJumperEntity jumper = ModEntities.PUDDLE_JUMPER.get().create(level);
        if (jumper == null) return InteractionResult.FAIL;

        Vec3 spawn = Vec3.atBottomCenterOf(pos);
        jumper.moveTo(spawn.x, spawn.y, spawn.z, player != null ? player.getYRot() : 0f, 0f);
        if (!level.noCollision(jumper, jumper.getBoundingBox())) {
            if (player != null && level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.puddlejumper.no_space"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.addFreshEntity(jumper);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.puddlejumper.puddle_jumper.desc").withStyle(ChatFormatting.GRAY));
    }
}
