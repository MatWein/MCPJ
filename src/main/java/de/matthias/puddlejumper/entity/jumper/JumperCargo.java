package de.matthias.puddlejumper.entity.jumper;

import de.matthias.puddlejumper.config.CargoConfig;
import de.matthias.puddlejumper.config.ServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public final class JumperCargo extends SimpleContainer {
    public static final int SLOTS = 54;
    private static final double MAX_USE_DISTANCE = 12;
    private static final double PICKUP_VERTICAL_RANGE = 0.5;
    private static final float PICKUP_VOLUME = 0.2f;
    private static final float PICKUP_PITCH_BASE = 2.0f;
    private static final float PICKUP_PITCH_SPREAD = 1.4f;
    private static final String CARGO_TAG = "Cargo";
    private static final String SLOT_TAG = "Slot";

    private final PuddleJumperEntity jumper;

    JumperCargo(PuddleJumperEntity jumper) {
        super(SLOTS);
        this.jumper = jumper;
    }

    @Override
    public boolean stillValid(Player player) {
        return jumper.isAlive() && player.distanceToSqr(jumper) < MAX_USE_DISTANCE * MAX_USE_DISTANCE;
    }

    void open(Player player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, user) -> ChestMenu.sixRows(id, inventory, this),
                Component.translatable("container.puddlejumper.cargo")));
        jumper.gameEvent(GameEvent.CONTAINER_OPEN, player);
    }

    void dropAll() {
        Containers.dropContents(jumper.level(), jumper, this);
    }

    void tick() {
        CargoConfig config = ServerConfig.CARGO;
        if (config.autoPickup() && jumper.tickCount % config.pickupIntervalTicks() == 0) pickUpNearbyItems(config.pickupRange());
    }

    private void pickUpNearbyItems(double range) {
        AABB area = jumper.fullBoundingBox().inflate(range, PICKUP_VERTICAL_RANGE, range);
        for (ItemEntity item : jumper.level().getEntitiesOfClass(ItemEntity.class, area, i -> i.isAlive() && !i.hasPickUpDelay())) {
            ItemStack stack = item.getItem();
            ItemStack rest = addItem(stack.copy());
            int taken = stack.getCount() - rest.getCount();
            if (taken <= 0) continue;
            announcePickup(item, taken);
            if (rest.isEmpty()) item.discard();
            else item.setItem(rest);
        }
    }

    private void announcePickup(ItemEntity item, int count) {
        // The vanilla pickup packet casts the collector to LivingEntity, so the animation targets the pilot.
        LivingEntity pilot = jumper.getControllingPassenger();
        if (pilot != null && jumper.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcast(item, new ClientboundTakeItemEntityPacket(item.getId(), pilot.getId(), count));
            return;
        }
        float pitch = (jumper.getRandom().nextFloat() - jumper.getRandom().nextFloat()) * PICKUP_PITCH_SPREAD + PICKUP_PITCH_BASE;
        jumper.level().playSound(null, item.getX(), item.getY(), item.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, PICKUP_VOLUME, pitch);
    }

    void load(CompoundTag tag) {
        clearContent();
        ListTag list = tag.getList(CARGO_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte(SLOT_TAG) & 0xFF;
            if (slot < getContainerSize()) setItem(slot, ItemStack.of(entry));
        }
    }

    void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte(SLOT_TAG, (byte) slot);
            stack.save(entry);
            list.add(entry);
        }
        tag.put(CARGO_TAG, list);
    }
}
