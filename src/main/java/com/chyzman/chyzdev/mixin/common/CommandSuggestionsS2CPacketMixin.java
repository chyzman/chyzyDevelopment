package com.chyzman.chyzdev.mixin.common;

import com.chyzman.chyzdev.command.suggestion.AdvancedSuggestion;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Debug(export = true, print = true)
@Mixin(CommandSuggestionsS2CPacket.class)
public abstract class CommandSuggestionsS2CPacketMixin {

    //TODO: somehow make this support vanilla clients

    @WrapOperation(method = "method_56609", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/suggestion/Suggestion;getText()Ljava/lang/String;", remap = false))
    private static String encodeAdvancedSuggestions(Suggestion instance, Operation<String> original) {
        if (instance instanceof AdvancedSuggestion advanced) return AdvancedSuggestion.Encoder.from(advanced).toJson();
        return original.call(instance);
    }

    @ModifyExpressionValue(method = "getSuggestions", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"))
    private static List<Suggestion> decodeAdvancedSuggestions(List<Suggestion> original, @Local StringRange range) {
        var newList = new ArrayList<Suggestion>();
        for (Suggestion suggestion : original) {
            if (AdvancedSuggestion.Encoder.isValid(suggestion.getText())) {
                AdvancedSuggestion.Encoder data = AdvancedSuggestion.Encoder.fromJson(suggestion.getText());
                if (data != null) {
                    newList.add(new AdvancedSuggestion(
                        range,
                        data.display(),
                        data.completion(),
                        data.aliases(),
                        suggestion.getTooltip()
                    ));
                    continue;
                }
            }
            newList.add(suggestion);
        }
        return newList;
    }
}
