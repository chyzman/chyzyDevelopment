package com.chyzman.chyzdev.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class RegistryEntriesArgumentType implements ArgumentType<RegistryEntriesSelector> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "foo:bar namespace minecraft", "minecraft:item filter #minecraft:dirt");

    private final CommandRegistryAccess registryAccess;

    public RegistryEntriesArgumentType(CommandRegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
    }

    public static RegistryEntriesArgumentType registryEntries(CommandRegistryAccess registryAccess) {
        return new RegistryEntriesArgumentType(registryAccess);
    }

    public static RegistryEntriesSelector getRegistryEntries(CommandContext<?> context, String name) {
        return context.getArgument(name, RegistryEntriesSelector.class);
    }

    @Override
    public RegistryEntriesSelector parse(StringReader reader) throws CommandSyntaxException {
        var selectorReader = new RegistryEntriesSelector.Reader(registryAccess);
        return selectorReader.read(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var stringReader = new StringReader(builder.getInput());
        stringReader.setCursor(builder.getStart());
        var selectorReader = new RegistryEntriesSelector.Reader(registryAccess);

        try {
            selectorReader.read(stringReader);
        } catch (CommandSyntaxException ignored) {
        }

        return selectorReader.listSuggestions(builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
