package com.chyzman.chyzdev.mixin.common;

import com.chyzman.chyzdev.pond.CommandSourceDuck;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(CommandContext.class)
public abstract class CommandContextMixin<S> {

    @Shadow(remap = false) public abstract S getSource();

    @ModifyReceiver(method = "getArgument", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private <V> Map<String, ParsedArgument<?, ?>> chyzdev$tryGetPackedArgument(
        Map<String, ParsedArgument<?, ?>> instance,
        Object o,
        @Local(argsOnly = true) String string,
        @Local(argsOnly = true) Class<V> type
    ) {
        if (instance.containsKey(string)) return instance;
        if (!(getSource() instanceof CommandSourceDuck sourceDuck)) return instance;
        return sourceDuck.chyzdev$getPackedArguments();
    }
}
