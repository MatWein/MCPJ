package de.mcpj.mc.entity.drone;

import de.mcpj.core.util.SaveData;
import de.mcpj.mc.util.NbtSaveData;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * Minecraft 26.3's side of {@link DroneEntity}: synced data and saving, which differ between Minecraft
 * versions. The drone itself is version-independent.
 */
public abstract class DroneEntityCompat extends Projectile {
    protected DroneEntityCompat(EntityType<? extends DroneEntityCompat> type, Level level) {
        super(type, level);
    }

    protected abstract void loadDrone(SaveData data);

    protected abstract void saveDrone(SaveData data);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        saveDrone(NbtSaveData.writing(output));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        loadDrone(NbtSaveData.reading(input));
    }
}
