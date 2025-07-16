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
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
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

    private <T> T getArgumentOr(CommandContext<T> context, String name, Class<T> type, T defaultValue) {
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
            .then(registryBranch())
            .then(literal("lang")
                      .executes(context -> dumpTranslations((CommandSourceDuck) context.getSource(), true, true))
                      .then(literal("used").executes(context -> dumpTranslations((CommandSourceDuck) context.getSource(), true, false)))
                      .then(literal("unused").executes(context -> dumpTranslations((CommandSourceDuck) context.getSource(), false, true)))
            )
            .then(literal("namespace")
                      .executes(context -> dumpNamespaces(
                          (CommandSourceDuck) context.getSource(),
                          ((CommandSourceDuck) context.getSource()).chyzdev$getResourceManager().getAllNamespaces()
                      ))
            )
            .then(literal("system-details")
                      .executes(context -> {
                          var source = (CommandSourceDuck) context.getSource();
                          var details = source.chyzdev$getSystemDetails();
                          var builder = new DumpBuilder()
                              .header(Text.translatable("dump.chyzdev.system_details.header"));
                          builder
                              .writeLn(details.collect())
                              .separator()
                              .sendAsFeedback(source);
                          return 1;
                      })
            );
    }

    //region REGISTRY

    private LiteralArgumentBuilder<C> registryBranch() {
        var registryNode = argument("registry", RegistryKeyArgumentType.registryKey(RegistriesAccessor.getROOT().getKey()))
            .suggests((context, builder) -> CommandSourceExtension.suggest(
                registryAccess.streamAllRegistryKeys(),
                builder,
                registryKey -> AdvancedSuggestion.builder(registryKey.getValue().toString())
                    .alias(registryKey.getValue().getPath())
                    .display(completion -> completion + " (" + registryAccess.getWrapperOrThrow(registryKey).streamKeys().count() + ")")
            ))
            .executes(context -> {
                ((CommandSourceDuck) context.getSource()).chyzdev$sendFeedback(Text.literal(getRegistry(context).toString()));
                return 1;
            }).build();

        registryNode.addChild(
            literal("namespace")
                .then(argument("namespace", StringArgumentType.word())
                          .suggests((context, builder) -> CommandSourceExtension.suggest(
                              getRegistry(context).streamKeys().collect(Collectors.groupingBy(key -> key.getValue().getNamespace())).entrySet().stream(),
                              builder,
                              entry -> AdvancedSuggestion.builder(entry.getKey())
                                  .display(completion -> completion + " (" + entry.getValue().size() + ")")
                          ))
                          .redirect(registryNode)
                ).build()
        );

        registryNode.addChild(
            literal("filter")
                .then(
                    argument("filter", new DynamicRegistryEntryPredicateArgumentType())
                        .suggests((context, builder) -> DynamicRegistryEntryPredicateArgumentType.listSuggestions(context, builder, getRegistry(context)))
                        .redirect(registryNode)
                ).build()
        );

        return literal("registry").then(registryNode);
    }

    private RegistryWrapper.Impl<C> getRegistry(CommandContext<C> context) {
        return registryAccess.getWrapperOrThrow((RegistryKey<MutableRegistry<C>>) context.getArgument("registry", RegistryKey.class));
    }

    public <T> int dumpRegistryInfo(
        CommandSourceDuck source,
        Registry<T> registry,
        @Nullable String namespace,
        @Nullable TagKey<T> tagKey
    ) {
        var builder = new DumpBuilder()
            .header(Text.translatable("dump.chyzdev.registry.header", registry.getKey().getValue().toString()));
        registry.getIds().stream()
            .collect(Collectors.groupingBy(Identifier::getNamespace))
            .entrySet().stream()
            .sorted(Comparator.comparingInt(entry -> entry.getValue().size()))
            .forEach(entry -> builder.writeList(Text.literal(entry.getKey()), entry.getValue()));
        builder
            .separator()
            .sendAsFeedback(source);
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
