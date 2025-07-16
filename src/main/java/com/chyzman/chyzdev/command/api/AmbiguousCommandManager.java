package com.chyzman.chyzdev.command.api;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.CommandSource;

public final class AmbiguousCommandManager<T extends CommandSource> {

    public LiteralArgumentBuilder<T> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public <S> RequiredArgumentBuilder<T, S> argument(String name, ArgumentType<S> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
