package de.mcpj.mc.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import de.mcpj.core.projector.JumperProjector;
import de.mcpj.core.projector.ProjectorHost;
import de.mcpj.mc.entity.jumper.PuddleJumperEntity;
import de.mcpj.mc.platform.McpjPlatform;
import de.mcpj.mc.util.NbtSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The jumper projector in the world; its logic is the core's {@link JumperProjector}. Energy from cables comes in
 * through the loader's energy API ({@link #logic()} is an EnergyInput).
 */
public class JumperProjectorBlockEntity extends BlockEntity implements ProjectorHost {
    private static final int AMBIENT_SOUND_INTERVAL = 80;
    private static final String JUMPER_TAG = "Jumper";

    private final JumperProjector projector = new JumperProjector(this);
    @Nullable private UUID jumperId;

    public JumperProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(McpjPlatform.get().projectorType(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JumperProjectorBlockEntity entity) {
        entity.projector.serverTick(level.hasNeighborSignal(pos));
        if (entity.projector.isBuilding() && level.getGameTime() % AMBIENT_SOUND_INTERVAL == 0) entity.playSound(SoundEvents.BEACON_AMBIENT);
    }

    public JumperProjector logic() {
        return projector;
    }

    /** Whether this projector is building that jumper. */
    public boolean isBuilding(Entity jumper) {
        return projector.isBuilding() && jumper.getUUID().equals(jumperId);
    }

    @Nullable
    private PuddleJumperEntity jumper() {
        return jumperId != null && level instanceof ServerLevel serverLevel && serverLevel.getEntity(jumperId) instanceof PuddleJumperEntity jumper
                && jumper.isAlive() ? jumper : null;
    }

    private Direction facing() {
        return getBlockState().getValue(JumperProjectorBlock.FACING);
    }

    private void playSound(SoundEvent sound) {
        if (level != null) level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1f, 1f);
    }

    // ------------------------------------------------------------------ ProjectorHost

    @Override
    public boolean isSpaceFree() {
        return level != null && JumperProjectorSite.isSpaceFree(level, worldPosition, facing(), jumper());
    }

    @Override
    public boolean startJumper() {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        PuddleJumperEntity jumper = JumperProjectorSite.spawnJumper(serverLevel, worldPosition, facing());
        if (jumper == null) return false;
        jumperId = jumper.getUUID();
        playSound(SoundEvents.BEACON_ACTIVATE);
        return true;
    }

    @Override
    public boolean hasJumper() {
        return jumper() != null;
    }

    @Override
    public void updateJumper(float progress) {
        PuddleJumperEntity jumper = jumper();
        if (jumper != null) jumper.construction().update(progress);
    }

    @Override
    public void finishJumper() {
        PuddleJumperEntity jumper = jumper();
        if (jumper != null) jumper.construction().finish();
        jumperId = null;
        playSound(SoundEvents.BEACON_POWER_SELECT);
    }

    @Override
    public void setBuilding(boolean building) {
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(JumperProjectorBlock.BUILDING, building), Block.UPDATE_ALL);
        if (!building) jumperId = null;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        projector.load(NbtSaveData.reading(tag));
        jumperId = tag.hasUUID(JUMPER_TAG) ? tag.getUUID(JUMPER_TAG) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        projector.save(NbtSaveData.writing(tag));
        if (jumperId != null) tag.putUUID(JUMPER_TAG, jumperId);
    }
}
