package com.chyzman.chyzdev.mixin.common.accessor;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(CommandContext.class)
public interface CommandContextAccessor<S> {

    @Accessor(value = "arguments", remap = false)
    Map<String, ParsedArgument<S, ?>> chyzdev$getArguments();
}
