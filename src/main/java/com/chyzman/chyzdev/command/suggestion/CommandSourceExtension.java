package com.chyzman.chyzdev.command.suggestion;

import com.chyzman.chyzdev.mixin.common.accessor.SuggestionsBuilderAccessor;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class CommandSourceExtension {
    public static <T> CompletableFuture<Suggestions> suggest(
        SuggestionsBuilder builder,
        Consumer<Consumer<T>> iterator,
        Function<T, AdvancedSuggestion.Builder> suggestionBuilderFunction
    ) {
        iterator.accept(t -> suggest(builder, suggestionBuilderFunction.apply(t)));
        return builder.buildFuture();
    }

    public static <T,S> CompletableFuture<Suggestions> suggest(
        SuggestionsBuilder builder,
        Consumer<BiConsumer<T,S>> iterator,
        BiFunction<T,S, AdvancedSuggestion.Builder> suggestionBuilderFunction
    ) {
        iterator.accept((t, s) -> suggest(builder, suggestionBuilderFunction.apply(t,s)));
        return builder.buildFuture();
    }

    private static void suggest(
        SuggestionsBuilder builder,
        @Nullable AdvancedSuggestion.Builder suggestionBuilder
    ) {
        if (suggestionBuilder == null) return;
        var suggestion = suggestionBuilder.build(StringRange.between(builder.getStart(), builder.getInput().length()));
        if (suggestion.matches(builder.getRemaining())) ((SuggestionsBuilderAccessor)builder).chyzdev$getResult().add(suggestion);
    }
}
