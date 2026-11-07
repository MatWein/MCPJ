package de.mcpj.mc.entity.drone;

import de.mcpj.core.util.SaveData;
import de.mcpj.mc.util.NbtSaveData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * Minecraft 1.20.1's side of {@link DroneEntity}: synced data and saving, which differ between Minecraft
 * versions. The drone itself is version-independent.
 */
public abstract class DroneEntityCompat extends Projectile {
    protected DroneEntityCompat(EntityType<? extends DroneEntityCompat> type, Level level) {
        super(type, level);
    }

    protected abstract void loadDrone(SaveData data);

    protected abstract void saveDrone(SaveData data);

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveDrone(NbtSaveData.writing(tag));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadDrone(NbtSaveData.reading(tag));
    }
}
