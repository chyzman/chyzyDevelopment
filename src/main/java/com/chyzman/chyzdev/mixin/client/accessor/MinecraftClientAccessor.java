package com.chyzman.chyzdev.mixin.client.accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.world.level.LevelInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.file.Path;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Invoker("addSystemDetailsToCrashReport")
    static SystemDetails chyzdev$addSystemDetailsToCrashReport(
        SystemDetails systemDetails,
        @Nullable MinecraftClient client,
        @Nullable LanguageManager languageManager,
        String version,
        @Nullable GameOptions options
    ) {
        throw new UnsupportedOperationException();
    }
}
