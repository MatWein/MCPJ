package de.mcpj.mc.gametest;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** What the shared game tests need from a loader; each loader sets it before its tests run ({@link #set}). */
public interface GameTestHooks {
    /** Changes the entity's dimension to exactly that spot (no portal search), the way portal mods do. */
    @Nullable
    Entity changeDimension(Entity entity, ServerLevel destination, Vec3 position, float yaw);

    /** Whether the entity may change to that dimension on its own (seated passengers may not). */
    boolean mayTravelAlone(Entity entity, ResourceKey<Level> destination);

    /** Fills a charging pad through the loader's energy API until it accepts no more; returns the stored energy. */
    int fillChargingPad(BlockEntity pad, int maxPerInsert);

    static GameTestHooks get() {
        GameTestHooks hooks = Holder.instance;
        if (hooks == null) throw new IllegalStateException("Game test hooks not set");
        return hooks;
    }

    static void set(GameTestHooks hooks) {
        Holder.instance = hooks;
    }

    final class Holder {
        private static GameTestHooks instance;

        private Holder() {}
    }
}
