package com.chyzman.chyzdev.mixin.common.accessor;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegistryKeyArgumentType.class)
public interface RegistryKeyArgumentTypeAccessor {
    @Invoker("getKey")
    static <T> RegistryKey<T> chyzdev$getKey(CommandContext<ServerCommandSource> context, String name, RegistryKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) {
        throw new IllegalArgumentException("This code is literally uncallable");
    }

    @Invoker("getRegistry")
    static <T> Registry<T> chyzdev$getRegistry(CommandContext<ServerCommandSource> context, RegistryKey<? extends Registry<T>> registryRef) {
        throw new IllegalArgumentException("This code is literally uncallable");
    }

    @Invoker("getRegistryEntry")
    static <T> RegistryEntry.Reference<T> chyzdev$getRegistryEntry(CommandContext<ServerCommandSource> context, String name, RegistryKey<Registry<T>> registryRef, DynamicCommandExceptionType invalidException) {
        throw new IllegalArgumentException("This code is literally uncallable");
    }
}
