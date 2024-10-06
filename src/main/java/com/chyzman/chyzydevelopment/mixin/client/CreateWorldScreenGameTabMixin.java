package com.chyzman.chyzydevelopment.mixin.client;

import com.chyzman.chyzydevelopment.mixin.client.accessor.CreateWorldScreenAccessor;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.chyzman.chyzydevelopment.client.ChyzydevelopmentClient.CHYZ_FLAT;
import static net.minecraft.world.gen.FlatLevelGeneratorPresets.THE_VOID;

@Mixin(CreateWorldScreen.GameTab.class)
public class CreateWorldScreenGameTabMixin {
    @Unique private static boolean VOID_TESTING = false;
    @Unique private boolean shouldBeVoid = false;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void injectTestingButton(
            CreateWorldScreen worldScreen,
            CallbackInfo ci,
            @Local() GridWidget.Adder adder
    ) {
        shouldBeVoid = VOID_TESTING;
        VOID_TESTING = false;
        adder.add(
                ButtonWidget.builder(
                                Text.translatable("createWorld.chyzydevelopment.testing" + (shouldBeVoid ? ".void" : "")),
                                button -> {
                                    setupTesting(worldScreen);
//                                    button.setMessage(Text.translatable("createWorld.chyzydevelopment.testing" + (shouldBeVoid ? ".void" : "")));
                                }
                        )
                        .width(210)
                        .build()
        );
    }

    @Unique
    private void setupTesting(CreateWorldScreen worldScreen) {
        var creator = worldScreen.getWorldCreator();

        if (creator.getWorldName().equals(Text.translatable("selectWorld.newWorld").getString())) creator.setWorldName(Text.translatable("selectWorld.chyzdevelopment.testing").getString());

        creator.setGameMode(WorldCreator.Mode.CREATIVE);

        creator.setCheatsEnabled(true);

        creator.setGenerateStructures(false);
        creator.setBonusChestEnabled(false);

        var rules = creator.getGameRules();

        rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, null);
        rules.get(GameRules.DO_WEATHER_CYCLE).set(false, null);

        rules.get(GameRules.KEEP_INVENTORY).set(true, null);
        rules.get(GameRules.DO_IMMEDIATE_RESPAWN).set(true, null);

        rules.get(GameRules.DO_MOB_SPAWNING).set(false, null);

        rules.get(GameRules.COMMAND_BLOCK_OUTPUT).set(false, null);

        creator.setGameRules(rules);

        creator.applyModifier((registryManager, registryHolder) -> {
            var preset = registryManager.get(RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET).get(shouldBeVoid ? THE_VOID : CHYZ_FLAT);
            if (preset == null) return registryHolder;
            return registryHolder.with(registryManager, new FlatChunkGenerator(preset.settings()));
        });

        var client = MinecraftClient.getInstance();

        var newScreen = CreateWorldScreen.create(
                client,
                ((CreateWorldScreenAccessor)worldScreen).chyzydevelopment$getParent(),
                ((CreateWorldScreenAccessor)worldScreen).chyzydevelopment$createLevelInfo(false),
                creator.getGeneratorOptionsHolder(),
                ((CreateWorldScreenAccessor)worldScreen).chyzydevelopment$getDataPackTempDir()
        );
        newScreen.getWorldCreator().setSeed("");

        VOID_TESTING = !shouldBeVoid;

        client.setScreen(newScreen);
    }
}
