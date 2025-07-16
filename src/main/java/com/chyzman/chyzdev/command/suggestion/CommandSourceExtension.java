package com.chyzman.chyzdev.command.suggestion;

import com.chyzman.chyzdev.mixin.accessor.SuggestionsBuilderAccessor;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

public class CommandSourceExtension {
    public static <T> CompletableFuture<Suggestions> suggest(Iterable<T> candidates, SuggestionsBuilder builder, Function<T, AdvancedSuggestion.Builder> suggestionBuilderFunction) {
        candidates.forEach(candidate -> suggest(candidate, builder, suggestionBuilderFunction));
        return builder.buildFuture();
    }

    public static <T> CompletableFuture<Suggestions> suggest(Stream<T> candidates, SuggestionsBuilder builder, Function<T, AdvancedSuggestion.Builder> suggestionBuilderFunction) {
        candidates.forEach(candidate -> suggest(candidate, builder, suggestionBuilderFunction));
        return builder.buildFuture();
    }

    private static <T> void suggest(T candidate, SuggestionsBuilder builder, Function<T, AdvancedSuggestion.Builder> suggestionBuilderFunction) {
        AdvancedSuggestion.Builder suggestionBuilder = suggestionBuilderFunction.apply(candidate);
        if (suggestionBuilder == null) return;
        var suggestion = suggestionBuilder.build(StringRange.between(builder.getStart(), builder.getInput().length()));
        if (suggestion.matches(builder.getRemaining())) ((SuggestionsBuilderAccessor)builder).chyzdev$getResult().add(suggestion);
    }
}
