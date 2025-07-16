package com.chyzman.chyzdev.client;

import com.chyzman.chyzdev.command.DumpCommand;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;

import static com.chyzman.chyzdev.ChyzyDevelopment.id;

public class ChyzyDevelopmentClient implements ClientModInitializer {
    public static final RegistryKey<FlatLevelGeneratorPreset> CHYZ_FLAT = RegistryKey.of(RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET, id("chyz"));

    @Override
    public void onInitializeClient() {
        DumpCommand.registerClient();
    }
}
