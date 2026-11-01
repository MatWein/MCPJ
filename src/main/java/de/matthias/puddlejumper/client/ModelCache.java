package de.matthias.puddlejumper.client;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import static de.matthias.puddlejumper.PuddleJumperMod.MODID;

/** Lädt die OBJ-Modelle bei jedem Ressourcen-Reload (F3+T) neu. */
public final class ModelCache implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ModelCache INSTANCE = new ModelCache();

    @Nullable public static ObjModel jumper;
    @Nullable public static ObjModel drone;

    private ModelCache() {}

    @Override
    public void onResourceManagerReload(ResourceManager rm) {
        jumper = tryLoad(rm, "models/entity/puddle_jumper.obj");
        drone = tryLoad(rm, "models/entity/drone.obj");
    }

    @Nullable
    private static ObjModel tryLoad(ResourceManager rm, String path) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MODID, path);
        try {
            return ObjModel.load(rm, loc);
        } catch (Exception e) {
            LOGGER.error("Konnte Modell {} nicht laden", loc, e);
            return null;
        }
    }
}
