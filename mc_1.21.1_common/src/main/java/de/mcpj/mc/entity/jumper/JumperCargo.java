package de.mcpj.mc.entity.jumper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import de.mcpj.core.config.CargoConfig;
import de.mcpj.core.config.ServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
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
                Component.translatable("container.mcpj.cargo")));
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

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        clearContent();
        ContainerHelper.loadAllItems(tag.getCompound(CARGO_TAG), getItems(), registries);
    }

    void save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag cargo = new CompoundTag();
        ContainerHelper.saveAllItems(cargo, getItems(), registries);
        tag.put(CARGO_TAG, cargo);
    }
}
