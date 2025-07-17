package com.chyzman.chyzdev.pond;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import net.minecraft.command.CommandSource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.SystemDetails;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;

public interface CommandSourceDuck {
    void chyzdev$sendFeedback(Text message);

    ResourceManager chyzdev$getResourceManager();

    SystemDetails chyzdev$getSystemDetails();

    <T, S extends CommandSource> S chyzdev$addPredicate(Class<T> type, Predicate<T> predicate);

    <T> Predicate<T> chyzdev$getPredicate(Class<T> type);

    void chyzdev$clearPredicates();

    <T, S extends CommandSource> S chyzdev$tryPackArgument(String name, Class<T> type, CommandContext<S> context);

    Map<String, ParsedArgument<?, ?>> chyzdev$getPackedArguments();

}
