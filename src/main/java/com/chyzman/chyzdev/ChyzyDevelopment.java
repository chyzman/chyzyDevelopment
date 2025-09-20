package com.chyzman.chyzdev;

import com.chyzman.chyzdev.command.DumpCommand;
import com.chyzman.chyzdev.command.argument.RegistryEntriesArgumentType;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

public class ChyzyDevelopment implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MODID = "chyzdev";

    @Override
    public void onInitialize() {
        ArgumentTypeRegistry.registerArgumentType(
            id("registry_entries_selector"),
            RegistryEntriesArgumentType.class,
            ConstantArgumentSerializer.of(RegistryEntriesArgumentType::new)
        );

        DumpCommand.registerServer();
    }

    public static Identifier id(String path) {
        return Identifier.of(MODID, path);
    }
}
