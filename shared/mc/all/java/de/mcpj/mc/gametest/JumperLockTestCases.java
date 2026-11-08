package de.mcpj.mc.gametest;

import de.mcpj.mc.compat.McCompat;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.item.JumperKeyItem;
import de.mcpj.mc.platform.McpjPlatform;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static de.mcpj.mc.gametest.GameTestSupport.spawnJumper;
import static de.mcpj.mc.gametest.GameTestSupport.vulnerablePlayer;

/** Key and lock game tests, shared by the loaders (each registers them with its own annotations). */
public final class JumperLockTestCases {
    /** A slot other than the selected one: the key is carried, not held. */
    private static final int CARRIED_SLOT = 5;

    private JumperLockTestCases() {}

    /** The first click pairs and locks, the next ones open and close; strangers stay out while it is locked. */
    public static void keyLocksAndUnlocksTheJumper(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player owner = vulnerablePlayer(helper);
        Player stranger = vulnerablePlayer(helper);
        ItemStack key = giveKey(owner);

        useKey(jumper, owner);
        helper.assertTrue(jumper.core().lock().locked(), "First use did not lock");
        helper.assertTrue(jumper.getUUID().equals(JumperKeyItem.pairedJumper(key)), "Key is not paired");
        helper.assertTrue(!jumper.mayUse(stranger), "Stranger may use a locked jumper");
        jumper.interactWith(stranger, InteractionHand.MAIN_HAND);
        helper.assertTrue(stranger.getVehicle() != jumper, "Stranger boarded a locked jumper");
        helper.assertTrue(!stranger.startRiding(jumper), "Stranger got a seat in a locked jumper");

        useKey(jumper, owner);
        helper.assertTrue(!jumper.core().lock().locked(), "Second use did not unlock");
        jumper.interactWith(stranger, InteractionHand.MAIN_HAND);
        helper.assertTrue(stranger.getVehicle() == jumper, "Stranger could not board the open jumper");

        useKey(jumper, owner);
        helper.assertTrue(jumper.core().lock().locked(), "Third use did not lock again");
        helper.assertTrue(stranger.getVehicle() != jumper, "Locking left a stranger on board");
        helper.succeed();
    }

    /** Whoever carries the key gets in without opening the ship for everyone. */
    public static void carriedKeyBoardsLockedJumper(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player owner = vulnerablePlayer(helper);
        ItemStack key = giveKey(owner);
        useKey(jumper, owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        owner.getInventory().setItem(CARRIED_SLOT, key);
        jumper.interactWith(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(owner.getVehicle() == jumper, "Key holder could not board");
        helper.assertTrue(jumper.core().lock().locked(), "Boarding unlocked the jumper");
        helper.succeed();
    }

    /** A paired ship ignores other keys, and a paired key only fits its own ship. */
    public static void otherKeysDoNotFit(GameTestHelper helper) {
        PuddleJumperEntity first = spawnJumper(helper, 8, 2, 8, 0);
        PuddleJumperEntity second = spawnJumper(helper, 18, 2, 18, 0);
        Player owner = vulnerablePlayer(helper);
        ItemStack firstKey = giveKey(owner);
        useKey(first, owner);
        useKey(first, owner);
        helper.assertTrue(!first.core().lock().locked(), "Jumper did not open");

        Player thief = vulnerablePlayer(helper);
        ItemStack blank = giveKey(thief);
        useKey(first, thief);
        helper.assertTrue(JumperKeyItem.pairedJumper(blank) == null, "Blank key claimed a paired jumper");
        helper.assertTrue(!first.core().lock().locked(), "Blank key changed the lock");

        useKey(second, owner);
        helper.assertTrue(first.getUUID().equals(JumperKeyItem.pairedJumper(firstKey)), "Key changed its jumper");
        helper.assertTrue(!second.core().lock().locked(), "Key locked another jumper");
        helper.succeed();
    }

    /** A key dropped beside the ship must not end up in its cargo hold, locked in. */
    public static void cargoDoesNotSwallowItsKey(GameTestHelper helper) {
        PuddleJumperEntity jumper = spawnJumper(helper, 12, 2, 12, 0);
        Player owner = vulnerablePlayer(helper);
        ItemStack key = giveKey(owner);
        useKey(jumper, owner);
        // beside the hull, within pickup range, without the random spawn velocity of item entities (see picksUpNearbyItems)
        Vec3 beside = helper.absoluteVec(new Vec3(13.9, 2, 12));
        ItemEntity dropped = new ItemEntity(helper.getLevel(), beside.x, beside.y, beside.z, key.copy(), 0, 0, 0);
        helper.getLevel().addFreshEntity(dropped);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(dropped.isAlive(), "The jumper picked up its own key");
            dropped.discard();
            helper.succeed();
        });
    }

    private static ItemStack giveKey(Player player) {
        ItemStack key = new ItemStack(McpjPlatform.get().jumperKeyItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        return key;
    }

    private static void useKey(PuddleJumperEntity jumper, Player player) {
        jumper.interactWith(player, InteractionHand.MAIN_HAND);
    }
}
