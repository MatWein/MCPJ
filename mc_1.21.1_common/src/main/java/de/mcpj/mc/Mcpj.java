package de.mcpj.mc;

import net.minecraft.resources.ResourceLocation;

/** Mod id and resource locations. */
public final class Mcpj {
    public static final String MOD_ID = "mcpj";

    private Mcpj() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
