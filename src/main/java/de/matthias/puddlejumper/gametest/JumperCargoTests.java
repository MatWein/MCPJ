package de.matthias.puddlejumper.gametest;

import de.matthias.puddlejumper.PuddleJumperMod;
import de.matthias.puddlejumper.entity.jumper.JumperCargo;
import de.matthias.puddlejumper.entity.jumper.PuddleJumperEntity;
import de.matthias.puddlejumper.registry.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static de.matthias.puddlejumper.gametest.GameTestSupport.PLATFORM;
import static de.matthias.puddlejumper.gametest.GameTestSupport.spawnJumper;

@GameTestHolder(PuddleJumperMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class JumperCargoTests {

    @GameTest(template = PLATFORM)
    public static void cargoSurvivesSave(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        jumper.cargo().setItem(0, new ItemStack(Items.DIAMOND, 17));
        jumper.cargo().setItem(JumperCargo.SLOTS - 1, new ItemStack(Items.ENDER_PEARL, 3));
        jumper.setCloaked(true);
        CompoundTag tag = jumper.saveWithoutId(new CompoundTag());

        PuddleJumperEntity copy = ModEntities.PUDDLE_JUMPER.get().create(helper.getLevel());
        helper.assertTrue(copy != null, "Could not create jumper");
        copy.load(tag);
        ItemStack first = copy.cargo().getItem(0);
        ItemStack last = copy.cargo().getItem(JumperCargo.SLOTS - 1);
        helper.assertTrue(first.is(Items.DIAMOND) && first.getCount() == 17, "First slot wrong");
        helper.assertTrue(last.is(Items.ENDER_PEARL) && last.getCount() == 3, "Last slot wrong");
        helper.assertTrue(copy.isCloaked(), "Cloak state not saved");
        helper.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void picksUpNearbyItems(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        // hull edge at x 13.43 (half width 1.43), pickup range 1 block: half a block beside the hull, without the
        // random spawn velocity of item entities so the item cannot drift out of range
        Vec3 beside = helper.absoluteVec(new Vec3(13.9, 2, 12));
        ItemEntity item = new ItemEntity(helper.getLevel(), beside.x, beside.y, beside.z, new ItemStack(Items.EMERALD, 5), 0, 0, 0);
        helper.getLevel().addFreshEntity(item);
        helper.succeedWhen(() -> {
            helper.assertTrue(item.isRemoved(), "Item was not picked up");
            helper.assertTrue(jumper.cargo().countItem(Items.EMERALD) == 5, "Cargo does not contain the emeralds");
        });
    }
}
