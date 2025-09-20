package com.chyzman.chyzdev.pond;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import net.minecraft.command.CommandSource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.SystemDetails;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public interface CommandSourceDuck {
    void chyzdev$sendFeedback(Text message);

    ResourceManager chyzdev$getResourceManager();

    SystemDetails chyzdev$getSystemDetails();

    //region PREDICATES

    Map<Class<?>, Deque<Predicate<?>>> chyzdev$getPredicateStacks();

    default <T, S extends CommandSource> S chyzdev$pushPredicate(Class<T> type, Predicate<T> predicate) {
        chyzdev$getPredicateStacks()
            .computeIfAbsent(type, k -> new ArrayDeque<>())
            .push(predicate);
        return (S) this;
    }

    default <T, S extends CommandSource> S chyzdev$popPredicate(Class<T> type) {
        Deque<Predicate<?>> stack = chyzdev$getPredicateStacks().get(type);
        if (stack != null && !stack.isEmpty()) stack.pop();
        return (S) this;
    }

    default <T> Predicate<T> chyzdev$getPredicate(Class<T> type) {
        Deque<Predicate<?>> stack = chyzdev$getPredicateStacks().get(type);
        if (stack == null || stack.isEmpty()) return o -> true;
        return stack.stream()
            .map(p -> (Predicate<T>) p)
            .reduce(o -> true, Predicate::and);
    }

    default <T, S extends CommandSource> S chyzdev$clearPredicate(Class<T> type) {
        Deque<Predicate<?>> stack = chyzdev$getPredicateStacks().get(type);
        if (stack != null) stack.clear();
        return (S) this;
    }

    default <S extends CommandSource> S chyzdev$clearAllPredicates() {
        chyzdev$getPredicateStacks().clear();
        return (S) this;
    }

    //endregion

    //region ARGUMENT PACKING

    <T, S extends CommandSource> S chyzdev$tryPackArgument(String name, Class<T> type, CommandContext<S> context);

    Map<String, ParsedArgument<?, ?>> chyzdev$getPackedArguments();

    //endregion

}
