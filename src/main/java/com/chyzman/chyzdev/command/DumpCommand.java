package com.chyzman.chyzdev.command;

import com.chyzman.chyzdev.ChyzyDevelopment;
import com.chyzman.chyzdev.command.api.DumpBuilder;
import com.chyzman.chyzdev.command.argument.DynamicRegistryEntryPredicateArgumentType;
import com.chyzman.chyzdev.command.suggestion.AdvancedSuggestion;
import com.chyzman.chyzdev.command.suggestion.CommandSourceExtension;
import com.chyzman.chyzdev.pond.CommandSourceDuck;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.mixin.registry.sync.RegistriesAccessor;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DumpCommand<C extends CommandSource> {
    public static final DynamicCommandExceptionType INVALID_REGISTRY_EXCEPTION = new DynamicCommandExceptionType(
        id -> Text.stringifiedTranslatable("commands.chyzdev.registry.notFound", id)
    );

    private final CommandDispatcher<C> dispatcher;
    private final CommandRegistryAccess registryAccess;

    private DumpCommand(CommandDispatcher<C> dispatcher, CommandRegistryAccess registryAccess) {
        this.dispatcher = dispatcher;
        this.registryAccess = registryAccess;
    }

    private LiteralArgumentBuilder<C> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private <S> RequiredArgumentBuilder<C, S> argument(String name, ArgumentType<S> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private <T> T getArgumentOr(CommandContext<C> context, String name, Class<T> type, T defaultValue) {
        try {
            return context.getArgument(name, type);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public static void registerServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(new DumpCommand<>(dispatcher, registryAccess).registerServerInternal()));
    }

    public static void registerClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(new DumpCommand<>(dispatcher, registryAccess).registerClientInternal()));
    }

    private LiteralArgumentBuilder<C> register(String suffix) {
        return literal(ChyzyDevelopment.MODID + suffix)
            .then(commonBranch());
    }

    private LiteralArgumentBuilder<C> registerServerInternal() {
        return register("");
    }

    private LiteralArgumentBuilder<C> registerClientInternal() {
        return register("-client");
    }


    private LiteralArgumentBuilder<C> commonBranch() {
        return literal("dump")
            .then(registryBranch());
//            .then(literal("lang")
//                      .executes(context -> dumpTranslations(context.getSource(), true, true))
//                      .then(literal("used").executes(context -> dumpTranslations(context.getSource(), true, false)))
//                      .then(literal("unused").executes(context -> dumpTranslations(context.getSource(), false, true)))
//            )
//            .then(literal("namespace")
//                      .executes(context -> dumpNamespaces(
//                          context.getSource(),
//                          context.getSource().chyzdev$getResourceManager().getAllNamespaces()
//                      ))
//            )
//            .then(literal("system-details")
//                      .executes(context -> {
//                          var source = (CommandSourceDuck) context.getSource();
//                          var details = source.chyzdev$getSystemDetails();
//                          var builder = new DumpBuilder()
//                              .header(Text.translatable("dump.chyzdev.system_details.header"));
//                          builder
//                              .writeLn(details.collect())
//                              .separator()
//                              .sendAsFeedback(source);
//                          return 1;
//                      })
//            );
    }

    //region REGISTRY

    private interface FilterBranch<C extends CommandSource> {
        ArgumentBuilder<C, ?> build(DumpCommand<C> commandInstance);

        Predicate<RegistryEntry.Reference<?>> getPredicate(CommandContext<C> context);

        default CommandContext<C> applyPredicate(CommandContext<C> context) {
            context.getSource()
                .chyzdev$addPredicate(RegistryEntry.Reference.class, getPredicate(context)::test)
                .chyzdev$tryPackArgument("registry", RegistryKey.class, context);
            return context;
        }
    }

    private LiteralArgumentBuilder<C> registryBranch() {
        var baseNode = argument("registry", RegistryKeyArgumentType.registryKey(RegistriesAccessor.getROOT().getKey()))
            .suggests((context, builder) -> CommandSourceExtension.suggest(
                registryAccess.streamAllRegistryKeys(),
                builder,
                registryKey -> AdvancedSuggestion.builder(registryKey.getValue().toString())
                    .alias(registryKey.getValue().getPath())
                    .display(completion -> completion + " (" + registryAccess.getWrapperOrThrow(registryKey).streamKeys().count() + ")")
            ))
            .executes(this::dumpRegistry)
            .build();

        Map<String, FilterBranch<C>> branches = new HashMap<>();

        branches.put(
            "namespace",
            new FilterBranch<>() {
                @Override
                public ArgumentBuilder<C, ?> build(DumpCommand<C> commandInstance) {
                    return commandInstance.argument("namespace", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSourceExtension.suggest(
                            registryAccess.getWrapperOrThrow(getRegistry(context).getRegistryKey()).streamEntries().filter(context.getSource().chyzdev$getPredicate(RegistryEntry.Reference.class)).collect(Collectors.groupingBy(key -> key.registryKey().getValue().getNamespace())).entrySet().stream(),
                            builder,
                            entry -> AdvancedSuggestion.builder(entry.getKey())
                                .display(completion -> completion + " (" + entry.getValue().size() + ")")
                        ));
                }

                @Override
                public Predicate<RegistryEntry.Reference<?>> getPredicate(CommandContext<C> context) {
                    return entry -> entry.registryKey().getValue().getNamespace().equals(context.getArgument("namespace", String.class));
                }
            }
        );

        branches.put(
            "filter",
            new FilterBranch<>() {
                @Override
                public ArgumentBuilder<C, ?> build(DumpCommand<C> commandInstance) {
                    return commandInstance.argument("filter", new DynamicRegistryEntryPredicateArgumentType())
                        .suggests((context, builder) -> {
                            var registry = getRegistry(context);
                            var filteredEntries = registryAccess
                                .getWrapperOrThrow(registry.getRegistryKey())
                                .streamEntries()
                                .filter(context.getSource().chyzdev$getPredicate(RegistryEntry.Reference.class))
                                .toList();
                            var filteredTags = filteredEntries.stream().flatMap(RegistryEntry.Reference::streamTags).distinct().toList();
                            CommandSourceExtension.suggest(filteredTags, builder, tagKey ->
                                AdvancedSuggestion.builder("#" + tagKey.id().toString())
                                    .alias(tagKey.id().getPath())
                                    .display(completion -> completion + " (" + registry.getOrThrow(tagKey).stream().count() + ")")
                            );
                            return CommandSource.suggestIdentifiers(filteredEntries.stream().map(ref -> ref.registryKey().getValue()), builder);
                        });
                }

                @Override
                public Predicate<RegistryEntry.Reference<?>> getPredicate(CommandContext<C> context) {
                    return DynamicRegistryEntryPredicateArgumentType.getPredicate(context, "filter");
                }
            }
        );

        for (var entry : branches.entrySet()) {
            var branch = entry.getValue();
            var builder = branch.build(this);
            builder
                .executes(context -> dumpRegistry(branch.applyPredicate(context)))
                .redirect(baseNode, context -> branch.applyPredicate(context).getSource());
            var built = literal(entry.getKey()).then(builder).build();
            baseNode.addChild(built);
        }

        return literal("registry").then(baseNode);
    }

    private RegistryWrapper.Impl<C> getRegistry(CommandContext<C> context) {
        return registryAccess.getWrapperOrThrow((RegistryKey<MutableRegistry<C>>) context.getArgument("registry", RegistryKey.class));
    }

    public int dumpRegistry(CommandContext<C> context) {
        var registry = getRegistry(context);
        var source = context.getSource();

        var builder = new DumpBuilder();
//            .header(Text.translatable("dump.chyzdev.registry.header", registryKey.getValue().toString()));

        var list = registry
            .streamEntries()
            .filter(source.chyzdev$getPredicate(RegistryEntry.Reference.class))
            .map(ref -> ref.registryKey().getValue())
            .toList();

        builder.writeList(Text.translatable("dump.chyzdev.registry.header", registry.getRegistryKey().getValue().toString()), list);

//            .sorted(Comparator.comparingInt(entry -> entry..size()))
//            .forEach(entry -> builder.writeList(Text.literal(entry.registryKey().getValue().toString()), entry.));
        builder
            .separator()
            .sendAsFeedback(source);

        source.chyzdev$clearPredicates();
        return 1;
    }

//endregion

    public static int dumpTranslations(CommandSourceDuck sourceDuck, boolean used, boolean unused) {
        new DumpBuilder()
//            .writeList(
//                Text.translatable("dump.chyzdev.lang.header"),
//                CommonCache.USED_TRANSLATION_KEYS.entrySet().stream().sorted(Comparator.comparingInt(entry1 -> -entry1.getValue())).toList(),
//                entry -> Text.literal(entry.getKey() + " (" + entry.getValue() + ")")
//            )
            .separator()
            .sendAsFeedback(sourceDuck);
        return 1;
    }

    public static int dumpNamespaces(CommandSourceDuck sourceDuck, Collection<String> namespaces) {
        new DumpBuilder()
            .writeList(
                Text.translatable("dump.chyzdev.namespace.header"),
                namespaces.stream().distinct().sorted().toList(),
                Text::literal
            )
            .separator()
            .sendAsFeedback(sourceDuck);
        return 1;
    }
}
