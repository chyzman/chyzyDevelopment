package com.chyzman.chyzdev.command.argument;

import com.chyzman.chyzdev.command.suggestion.AdvancedSuggestion;
import com.chyzman.chyzdev.command.suggestion.CommandSourceExtension;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.mixin.registry.sync.RegistriesAccessor;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.mojang.brigadier.CommandDispatcher.ARGUMENT_SEPARATOR_CHAR;
import static net.minecraft.command.EntitySelectorReader.INVERT_MODIFIER;

public class RegistryEntriesSelector {
    public static final String INVERT_MODIFIER_STRING = String.valueOf(INVERT_MODIFIER);

    public final RegistryWrapper.Impl<?> registry;
    public final Predicate<RegistryEntry.Reference<?>> predicate;
    public final Comparator<RegistryEntry.Reference<?>> comparator = Comparator.comparing(ref -> ref.registryKey().getValue());

    public RegistryEntriesSelector(
        RegistryWrapper.Impl<?> registry,
        Predicate<RegistryEntry.Reference<?>> predicate,
        Comparator<RegistryEntry.Reference<?>> comparator
    ) {
        this.registry = registry;
        this.predicate = predicate;
        this.comparator = comparator;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class Reader {

        public static final SimpleCommandExceptionType MISSING_REGISTRY_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.chyzdev.registryEntrySelector.registry.missing"));

        public static final DynamicCommandExceptionType INVALID_REGISTRY_EXCEPTION = new DynamicCommandExceptionType(o -> Text.translatable("argument.chyzdev.registryEntrySelector.registry.invalid", o));

        public static final SimpleCommandExceptionType MISSING_OPTION_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.chyzdev.registryEntrySelector.option.missing"));

        public static final DynamicCommandExceptionType INVALID_OPTION_EXCEPTION = new DynamicCommandExceptionType(o -> Text.translatable("argument.chyzdev.registryEntrySelector.option.invalid", o));

        public static final DynamicCommandExceptionType REPEATED_OPTION_EXCEPTION = new DynamicCommandExceptionType(o -> Text.translatable("argument.chyzdev.registryEntrySelector.option.repeat", o));

        public static final DynamicCommandExceptionType MISSING_OPTION_VALUE_EXCEPTION = new DynamicCommandExceptionType(o -> Text.translatable("argument.chyzdev.registryEntrySelector.option.value", o));


        private final CommandRegistryAccess registryAccess;

        private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestionProvider = SuggestionsBuilder::buildFuture;

        private RegistryWrapper.Impl<?> registry = null;
        private Predicate<RegistryEntry.Reference<?>> predicate = Objects::nonNull;

        public Reader(CommandRegistryAccess registryAccess) {
            this.registryAccess = registryAccess;
        }

        public CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder) {
            return suggestionProvider.apply(builder);
        }

        protected Predicate<RegistryEntry.Reference<?>> addPredicate(
            Predicate<RegistryEntry.Reference<?>> predicate
        ) {
            this.predicate = this.predicate.and(predicate);
            return this.predicate;
        }

        protected void clearSuggestor() {
            suggestionProvider = SuggestionsBuilder::buildFuture;
        }

        public RegistryEntriesSelector read(StringReader reader) throws CommandSyntaxException {
            readRegistry(reader);

            if (shouldFinish(reader)) return build();

            readOptions(reader);

            return build();
        }

        private RegistryEntriesSelector build() {
            return new RegistryEntriesSelector(
                registry,
                predicate
            );
        }

        protected static boolean tryReadString(StringReader reader, String string) throws CommandSyntaxException {
            var i = reader.getCursor();
            var read = reader.readString();
            if (read.equals(string)) return true;
            reader.setCursor(i);
            return false;
        }

        protected static boolean shouldFinish(StringReader reader) throws CommandSyntaxException {
            if (!reader.canRead()) return true;
            expectSeparator(reader);
            return false;
        }

        protected static void expectSeparator(StringReader reader) throws CommandSyntaxException {
            try {
                reader.expect(ARGUMENT_SEPARATOR_CHAR);
            } catch (CommandSyntaxException e) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherExpectedArgumentSeparator().createWithContext(reader);
            }
        }

        //region REGISTRY

        protected void readRegistry(StringReader reader) throws CommandSyntaxException {
            this.suggestionProvider = this::suggestRegistries;

            if (!reader.canRead()) throw MISSING_REGISTRY_EXCEPTION.createWithContext(reader);

            if (tryReadString(reader, "root")) {
                registry = RegistriesAccessor.getROOT().getTagCreatingWrapper();
                return;
            }

            var cursor = reader.getCursor();
            var identifier = Identifier.fromCommandInput(reader);
            this.registry = registryAccess.getOptionalWrapper(RegistryKey.ofRegistry(identifier)).orElseThrow(() -> {
                reader.setCursor(cursor);
                return INVALID_REGISTRY_EXCEPTION.createWithContext(reader, identifier);
            });
        }

        private CompletableFuture<Suggestions> suggestRegistries(SuggestionsBuilder builder) {
            CommandSourceExtension.suggest(builder, registryAccess.streamAllRegistryKeys()::forEach, registryKey ->
                AdvancedSuggestion.builder(registryKey.getValue().toString())
                    .alias(registryKey.getValue().getPath())
                    .display(completion -> completion + " (" + registryAccess.getWrapperOrThrow(registryKey).streamKeys().count() + ")")
            );

            CommandSourceExtension.suggest(builder, List.of("root")::forEach, string ->
                AdvancedSuggestion.builder(string).display(completion -> completion + " (" + RegistriesAccessor.getROOT().stream().count() + ")")
            );

            return builder.buildFuture();
        }

        //endregion

        //region OPTION

        protected void readOptions(StringReader reader) throws CommandSyntaxException {
            Set<Option> seen = new HashSet<>();

            suggestionProvider = (builder -> suggestOptionTypes(builder.createOffset(reader.getCursor()), seen));

            while (reader.canRead()) {
                readOptionType(reader, seen).read(reader, this);
                clearSuggestor();
                if (shouldFinish(reader)) return;
                suggestionProvider = (builder -> suggestOptionTypes(builder.createOffset(reader.getCursor()), seen));
            }
        }

        protected Option readOptionType(StringReader reader, Set<Option> seen) throws CommandSyntaxException {
            suggestionProvider = (builder -> suggestOptionTypes(builder.createOffset(reader.getCursor()), seen));
            var cursor = reader.getCursor();
            var type = reader.readString();
            var option = OPTIONS.get(type);
            if (option == null || (!option.repeatable() && seen.contains(option))) {
                reader.setCursor(cursor);
                throw (option == null ? INVALID_OPTION_EXCEPTION : INVALID_REGISTRY_EXCEPTION).createWithContext(reader, type);
            }
            seen.add(option);
            clearSuggestor();
            expectSeparator(reader);
            return option;
        }


        private CompletableFuture<Suggestions> suggestOptionTypes(SuggestionsBuilder builder, Set<Option> seen) {
            var options = OPTIONS
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().repeatable() || !seen.contains(entry.getValue()))
                .filter(stringOptionEntry -> stringOptionEntry.getValue().canBeApplied(this))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            CommandSource.suggestMatching(options.keySet(), builder);

            return builder.buildFuture();
        }

        public static final Map<String, Option> OPTIONS = Map.of(
            "namespace",
            new Option() {
                private static final DynamicCommandExceptionType INVALID_NAMESPACE_EXCEPTION = new DynamicCommandExceptionType(o -> Text.translatable("argument.chyzdev.registryEntrySelector.option.namespace.invalid", o));

                @Override
                public void read(StringReader reader, Reader ctx) throws CommandSyntaxException {
                    var validNamespaces = ctx.registry.streamEntries()
                        .filter(ctx.predicate)
                        .collect(Collectors.groupingBy(ref -> ref.registryKey().getValue().getNamespace(), Collectors.counting()));

                    ctx.suggestionProvider = builder -> CommandSourceExtension.suggest(
                        builder.createOffset(reader.getCursor()), validNamespaces::forEach,
                        (namespace, count) -> AdvancedSuggestion.builder(namespace)
                            .display(completion -> completion + " (" + count + ")")
                    );

                    var cursor = reader.getCursor();
                    var namespace = reader.readString();

                    if (!validNamespaces.containsKey(namespace)) {
                        reader.setCursor(cursor);
                        throw INVALID_NAMESPACE_EXCEPTION.createWithContext(reader, namespace);
                    }

                    ctx.addPredicate(ref -> ref.registryKey().getValue().getNamespace().equals(namespace));
                }

                @Override
                public boolean canBeApplied(Reader ctx) {
                    return ctx.registry
                               .streamEntries()
                               .map(reference -> reference.registryKey().getValue().getNamespace())
                               .distinct()
                               .count() > 1;
                }
            },
            "tag",
            new Option() {
                private static final DynamicCommandExceptionType INVALID_TAG_EXCEPTION = new DynamicCommandExceptionType(o -> Text.translatable("argument.chyzdev.registryEntrySelector.option.tag.tag.invalid", o));

                @Override
                public void read(StringReader reader, Reader ctx) throws CommandSyntaxException {
                    var validEntries = ctx.registry
                        .streamEntries()
                        .filter(ctx.predicate)
                        .toList();

                    var entryCount = validEntries.size();

                    var validTags = validEntries
                        .stream()
                        .flatMap(ref -> ref.streamTags().map(tagKey -> new Pair<>(tagKey, ref)))
                        .collect(Collectors.groupingBy(Pair::getLeft, Collectors.counting()))
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() < entryCount)
                        .collect(Collectors.toMap(entry -> entry.getKey().id(), Map.Entry::getValue));

                    var offset = reader.getCursor();
                    ctx.suggestionProvider = builder -> {
                        var b = builder.createOffset(offset);
                        suggestTags(b, validTags, "");
                        b.suggest(INVERT_MODIFIER_STRING);
                        return b.buildFuture();
                    };

                    if (!reader.canRead()) throw MISSING_OPTION_VALUE_EXCEPTION.createWithContext(reader, "tag");
                    var inverted = reader.peek() == INVERT_MODIFIER;
                    if (inverted) reader.skip();
                    var inversion = inverted ? INVERT_MODIFIER_STRING : "";

                    ctx.suggestionProvider = builder -> {
                        var b = builder.createOffset(offset);
                        suggestTags(b, validTags, inversion);
                        return b.buildFuture();
                    };

                    var cursor = reader.getCursor();
                    var id = Identifier.fromCommandInput(reader);

                    if (!validTags.containsKey(id)) {
                        reader.setCursor(cursor);
                        throw INVALID_TAG_EXCEPTION.createWithContext(reader, id.toString());
                    }

                    Predicate<RegistryEntry.Reference<?>> predicate = ref -> ref.streamTags().anyMatch(tagKey -> tagKey.id().equals(id));

                    if (inverted) predicate = predicate.negate();

                    ctx.addPredicate(predicate);
                }

                @Override
                public boolean canBeApplied(Reader ctx) {
                    return ctx.registry
                        .streamEntries()
                        .anyMatch(ref -> ctx.predicate.test(ref) && ref.streamTags().findAny().isPresent());
                }

                @Override
                public boolean repeatable() {
                    return true;
                }

                private void suggestTags(SuggestionsBuilder builder, Map<Identifier, Long> tags, String prefix) {
                    CommandSourceExtension.suggest(
                        builder,
                        tags::forEach,
                        (tag, count) ->
                            AdvancedSuggestion.builder(prefix + tag.toString())
                                .alias(tag.getPath())
                                .display(completion -> completion + " (" + count + ")")
                    );
                }
            },
            "exclude",
            new Option() {
                private static final DynamicCommandExceptionType INVALID_ENTRY_EXCEPTION = new DynamicCommandExceptionType(o -> Text.translatable("argument.chyzdev.registryEntrySelector.option.exclude.entry.invalid", o));

                @Override
                public void read(StringReader reader, Reader ctx) throws CommandSyntaxException {
                    List<Identifier> identifiers = new ArrayList<>();
                    ctx.suggestionProvider = builder -> CommandSource.suggestIdentifiers(ctx.registry.streamEntries().filter(ctx.predicate).filter(ref -> !identifiers.contains(ref.registryKey().getValue())).map(ref -> ref.registryKey().getValue()).toList(), builder.createOffset(reader.getCursor()));

                    if (!reader.canRead() || reader.peek() == ',') throw MISSING_OPTION_VALUE_EXCEPTION.createWithContext(reader, "exclude");

                    do {
                        if (reader.peek() == ',') reader.skip();
                        var validEntries = ctx.registry
                            .streamEntries()
                            .filter(ctx.predicate)
                            .filter(ref -> !identifiers.contains(ref.registryKey().getValue()))
                            .map(ref -> ref.registryKey().getValue())
                            .toList();

                        var cursor = reader.getCursor();
                        ctx.suggestionProvider = builder -> CommandSource.suggestIdentifiers(validEntries, builder.createOffset(cursor));
                        var id = Identifier.fromCommandInputNonEmpty(reader);
                        if (!validEntries.contains(id)) {
                            reader.setCursor(cursor);
                            throw INVALID_ENTRY_EXCEPTION.createWithContext(reader, id);
                        }
                        identifiers.add(id);
                    } while (reader.canRead() && reader.peek() != ARGUMENT_SEPARATOR_CHAR);

                    Predicate<RegistryEntry.Reference<?>> predicate = ref -> !identifiers.contains(ref.registryKey().getValue());

                    ctx.addPredicate(predicate);
                }

                @Override
                public boolean repeatable() {
                    return true;
                }
            },
            "sort",
            new Option() {
                private static final DynamicCommandExceptionType INVALID_ENTRY_EXCEPTION = new DynamicCommandExceptionType(o -> Text.translatable("argument.chyzdev.registryEntrySelector.option.exclude.entry.invalid", o));

                @Override
                public void read(StringReader reader, Reader ctx) throws CommandSyntaxException {
                    List<Identifier> identifiers = new ArrayList<>();
                    ctx.suggestionProvider = builder -> CommandSource.suggestIdentifiers(ctx.registry.streamEntries().filter(ctx.predicate).filter(ref -> !identifiers.contains(ref.registryKey().getValue())).map(ref -> ref.registryKey().getValue()).toList(), builder.createOffset(reader.getCursor()));

                    if (!reader.canRead() || reader.peek() == ',') throw MISSING_OPTION_VALUE_EXCEPTION.createWithContext(reader, "exclude");

                    do {
                        if (reader.peek() == ',') reader.skip();
                        var validEntries = ctx.registry
                            .streamEntries()
                            .filter(ctx.predicate)
                            .filter(ref -> !identifiers.contains(ref.registryKey().getValue()))
                            .map(ref -> ref.registryKey().getValue())
                            .toList();

                        var cursor = reader.getCursor();
                        ctx.suggestionProvider = builder -> CommandSource.suggestIdentifiers(validEntries, builder.createOffset(cursor));
                        var id = Identifier.fromCommandInputNonEmpty(reader);
                        if (!validEntries.contains(id)) {
                            reader.setCursor(cursor);
                            throw INVALID_ENTRY_EXCEPTION.createWithContext(reader, id);
                        }
                        identifiers.add(id);
                    } while (reader.canRead() && reader.peek() != ARGUMENT_SEPARATOR_CHAR);

                    Predicate<RegistryEntry.Reference<?>> predicate = ref -> !identifiers.contains(ref.registryKey().getValue());

                    ctx.addPredicate(predicate);
                }

                @Override
                public boolean repeatable() {
                    return true;
                }
            }
        );

        public interface Option {
            void read(StringReader reader, Reader ctx) throws CommandSyntaxException;

            default boolean canBeApplied(Reader ctx) {
                return ctx.registry
                    .streamEntries()
                    .anyMatch(ctx.predicate);
            }

            default boolean repeatable() {
                return false;
            }
        }

        //endregion
    }
}
