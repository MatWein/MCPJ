package de.matthias.puddlejumper.item;

import de.matthias.puddlejumper.ModRegistry;
import de.matthias.puddlejumper.entity.PuddleJumperEntity;
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
    public PuddleJumperItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Player player = ctx.getPlayer();
        PuddleJumperEntity jumper = ModRegistry.PUDDLE_JUMPER.get().create(level);
        if (jumper == null) return InteractionResult.FAIL;
        Vec3 spawn = Vec3.atBottomCenterOf(pos);
        float yaw = player != null ? player.getYRot() : 0f;
        jumper.moveTo(spawn.x, spawn.y, spawn.z, yaw, 0f);
        if (!level.noCollision(jumper, jumper.getBoundingBox())) {
            if (player != null && level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.puddlejumper.no_space"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.addFreshEntity(jumper);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
            if (player == null || !player.getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.puddlejumper.puddle_jumper.desc").withStyle(ChatFormatting.GRAY));
    }
}
