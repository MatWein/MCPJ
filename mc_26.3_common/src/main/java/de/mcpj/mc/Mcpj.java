package de.mcpj.mc;

import net.minecraft.resources.Identifier;

/** Mod id and resource locations. */
public final class Mcpj {
    public static final String MOD_ID = "mcpj";

    private Mcpj() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
