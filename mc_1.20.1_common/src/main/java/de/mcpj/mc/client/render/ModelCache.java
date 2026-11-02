package de.mcpj.mc.client.render;

import de.mcpj.mc.Mcpj;
import com.mojang.logging.LogUtils;
import de.mcpj.core.render.ObjMesh;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ModelCache implements ResourceManagerReloadListener {
    public static final ModelCache INSTANCE = new ModelCache();
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable private static ObjMesh jumper;
    @Nullable private static ObjMesh drone;

    private ModelCache() {}

    @Nullable
    static ObjMesh jumper() {
        return jumper;
    }

    @Nullable
    static ObjMesh drone() {
        return drone;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        jumper = load(resources, "models/entity/puddle_jumper.obj");
        drone = load(resources, "models/entity/drone.obj");
    }

    @Nullable
    private static ObjMesh load(ResourceManager resources, String path) {
        ResourceLocation location = Mcpj.id(path);
        try {
            return ObjMesh.parse(new InputStreamReader(resources.open(location), StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.error("Could not load model {}", location, e);
            return null;
        }
    }
}
