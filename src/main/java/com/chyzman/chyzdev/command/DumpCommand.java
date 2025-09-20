package com.chyzman.chyzdev.command;

import com.chyzman.chyzdev.ChyzyDevelopment;
import com.chyzman.chyzdev.command.api.DumpBuilder;
import com.chyzman.chyzdev.command.argument.RegistryEntriesArgumentType;
import com.chyzman.chyzdev.command.argument.RegistryEntriesSelector;
import com.chyzman.chyzdev.pond.CommandSourceDuck;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.*;

public class DumpCommand<C extends CommandSource> {
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
        return literal("dump").then(
            literal("registry").then(
                argument("selector", RegistryEntriesArgumentType.registryEntries(registryAccess))
                    .executes(context -> dumpRegistry(
                        context.getSource(),
                        RegistryEntriesArgumentType.getRegistryEntries(context, "selector")
                    ))
            )
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
        );
    }

    public int dumpRegistry(C source, RegistryEntriesSelector selector) {
        var registry = selector.registry;

        var builder = new DumpBuilder();
//            .header(Text.translatable("dump.chyzdev.registry.header", registryKey.getValue().toString()));

        var list = registry
            .streamEntries()
            .filter(selector.predicate)
            .map(ref -> ref.registryKey().getValue())
            .toList();

        builder.writeList(Text.translatable("dump.chyzdev.registry.header", registry.getRegistryKey().getValue().toString()), list);

//            .sorted(Comparator.comparingInt(entry -> entry..size()))
//            .forEach(entry -> builder.writeList(Text.literal(entry.registryKey().getValue().toString()), entry.));
        builder
            .separator()
            .sendAsFeedback(source);
        return 1;
    }


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
