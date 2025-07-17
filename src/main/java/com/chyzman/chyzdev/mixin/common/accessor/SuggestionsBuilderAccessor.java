package com.chyzman.chyzdev.mixin.common.accessor;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SuggestionsBuilder.class)
public interface SuggestionsBuilderAccessor {

    @Accessor(value = "result", remap = false)
    List<Suggestion> chyzdev$getResult();
}
