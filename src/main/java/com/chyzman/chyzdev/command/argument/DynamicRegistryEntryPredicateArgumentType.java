package com.chyzman.chyzdev.command.argument;

import com.chyzman.chyzdev.command.suggestion.AdvancedSuggestion;
import com.chyzman.chyzdev.command.suggestion.CommandSourceExtension;
import com.chyzman.chyzdev.pond.CommandSourceDuck;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class DynamicRegistryEntryPredicateArgumentType implements ArgumentType<Predicate<RegistryEntry<?>>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");

    public DynamicRegistryEntryPredicateArgumentType() {}

    @SuppressWarnings("unchecked")
    public Predicate<RegistryEntry<?>> getPredicate(CommandContext<?> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, Predicate.class);
    }

    @Override
    public Predicate<RegistryEntry<?>> parse(StringReader stringReader) throws CommandSyntaxException {
        if (stringReader.canRead() && stringReader.peek() == '#') {
            var i = stringReader.getCursor();
            try {
                stringReader.skip();
                var identifier = Identifier.fromCommandInput(stringReader);
                return entry -> entry.streamTags().anyMatch(tagKey -> tagKey.id().equals(identifier));
            } catch (CommandSyntaxException e) {
                stringReader.setCursor(i);
                throw e;
            }
        } else {
            var identifier = Identifier.fromCommandInput(stringReader);
            return entry -> entry.matchesId(identifier);
        }
    }

    public static <T> CompletableFuture<Suggestions> listSuggestions(CommandContext<T> context, SuggestionsBuilder builder, RegistryWrapper.Impl<T> registry) {
        CommandSourceExtension.suggest(registry.streamTagKeys(), builder, tagKey ->
            AdvancedSuggestion.builder("#" + tagKey.id().toString())
                .alias(tagKey.id().getPath())
                .display(completion -> completion + " (" + registry.getOrThrow(tagKey).stream().count() + ")")
        );
        return CommandSource.suggestIdentifiers(registry.streamKeys().map(RegistryKey::getValue), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @FunctionalInterface
    public interface RegistryRefProvider extends BiFunction<CommandContext<?>, CommandRegistryAccess, RegistryKey<Registry<?>>> {
        @Override
        RegistryKey<Registry<?>> apply(CommandContext<?> context, CommandRegistryAccess registryAccess);
    }
}
