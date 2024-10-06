package com.chyzman.chyzydevelopment.mixin.client.accessor;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.file.Path;

@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenAccessor {
    @Invoker("createLevelInfo")
    LevelInfo chyzydevelopment$createLevelInfo(boolean debugWorld);

    @Accessor("dataPackTempDir")
    Path chyzydevelopment$getDataPackTempDir();

    @Accessor("parent")
    Screen chyzydevelopment$getParent();
}
