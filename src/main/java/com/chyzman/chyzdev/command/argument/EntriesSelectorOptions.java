package com.chyzman.chyzdev.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class EntriesSelectorOptions {

    public interface Option {
        void read(StringReader reader, RegistryEntriesSelector.Reader ctx) throws CommandSyntaxException;

        default boolean canBeApplied(RegistryEntriesSelector.Reader ctx) {
            return ctx.registry
                .streamEntries()
                .anyMatch(ctx.predicate);
        }

        default boolean repeatable() {
            return false;
        }
    }
}
