package com.chyzman.chyzdev.mixin.common;

import com.chyzman.chyzdev.mixin.common.accessor.CommandContextAccessor;
import com.chyzman.chyzdev.pond.CommandSourceDuck;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@Mixin({ClientCommandSource.class, ServerCommandSource.class})
public abstract class CommandSourceMixin implements CommandSourceDuck {
    @Unique private final Map<Class<?>, Predicate<Object>> PREDICATE_MAP = new HashMap<>();
    @Unique private final Map<String, ParsedArgument<?, ?>> PACKED_ARGS = new HashMap<>();

    @Override
    public <T, S extends CommandSource> S chyzdev$addPredicate(Class<T> type, Predicate<T> predicate) {
        PREDICATE_MAP.merge(
            type,
            (Predicate<Object>) predicate,
            Predicate::and
        );
        return (S) this;
    }

    @Override
    public <T> Predicate<T> chyzdev$getPredicate(Class<T> type) {
        return (Predicate<T>) PREDICATE_MAP.getOrDefault(type, o -> true);
    }

    @Override
    public void chyzdev$clearPredicates() {
        PREDICATE_MAP.clear();
    }

    @Override
    public <T, S extends CommandSource> S chyzdev$tryPackArgument(String name, Class<T> type, CommandContext<S> context) {
        ParsedArgument<CommandSource, ?> argument = (ParsedArgument<CommandSource, ?>) ((CommandContextAccessor)context).chyzdev$getArguments().get(name);
        if (argument != null) PACKED_ARGS.put(name, argument);
        return (S) this;
    }

    @Override
    public Map<String, ParsedArgument<?, ?>> chyzdev$getPackedArguments() {
        return new HashMap<>(PACKED_ARGS);
    }
}
