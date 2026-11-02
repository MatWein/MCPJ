package de.matthias.puddlejumper.client.render;

import com.mojang.logging.LogUtils;
import de.matthias.puddlejumper.PuddleJumperMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class ModelCache implements ResourceManagerReloadListener {
    public static final ModelCache INSTANCE = new ModelCache();
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable private static ObjModel jumper;
    @Nullable private static ObjModel drone;

    private ModelCache() {}

    @Nullable
    static ObjModel jumper() {
        return jumper;
    }

    @Nullable
    static ObjModel drone() {
        return drone;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        jumper = load(resources, "models/entity/puddle_jumper.obj");
        drone = load(resources, "models/entity/drone.obj");
    }

    @Nullable
    private static ObjModel load(ResourceManager resources, String path) {
        ResourceLocation location = PuddleJumperMod.id(path);
        try {
            return ObjModel.load(resources, location);
        } catch (Exception e) {
            LOGGER.error("Could not load model {}", location, e);
            return null;
        }
    }
}
