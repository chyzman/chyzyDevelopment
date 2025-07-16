package com.chyzman.chyzdev.mixin;

import com.chyzman.chyzdev.pond.CommandSourceDuck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.SystemDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceMixin implements CommandSourceDuck {
    @Shadow public abstract void sendFeedback(Supplier<Text> feedbackSupplier, boolean broadcastToOps);

    @Override
    public void chyzdev$sendFeedback(Text message) {
        sendFeedback(() -> message, false);
    }

    @Override
    public ResourceManager chyzdev$getResourceManager() {
        return ((ServerCommandSource)(Object)this).getServer().getResourceManager();
    }

    @Override
    public SystemDetails chyzdev$getSystemDetails() {
        return ((ServerCommandSource)(Object)this).getServer().addExtraSystemDetails(new SystemDetails());
    }
}
