package com.chyzman.chyzdev.mixin.common.accessor;

import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.SingleRedirectModifier;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandNode.class)
public interface CommandNodeAccessor<S> {

    @Accessor(value = "redirect", remap = false)
    void chyzdev$setRedirect(CommandNode<S> redirect);

    @Accessor(value = "modifier", remap = false)
    void chyzdev$setRedirectModifier(RedirectModifier<S> modifier);
}
