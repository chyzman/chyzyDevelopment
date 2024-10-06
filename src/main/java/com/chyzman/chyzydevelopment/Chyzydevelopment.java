package com.chyzman.chyzydevelopment;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class Chyzydevelopment implements ModInitializer {
    public static final String MODID = "chyzydevelopment";

    @Override
    public void onInitialize() {
    }

    public static Identifier id(String path) {
        return Identifier.of(MODID, path);
    }
}
