package com.chyzman.chyzdev.mixin.client;

import com.chyzman.chyzdev.mixin.client.accessor.MinecraftClientAccessor;
import com.chyzman.chyzdev.pond.CommandSourceDuck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.SystemDetails;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientCommandSource.class)
public class ClientCommandSourceMixin implements CommandSourceDuck {
    @Override
    public void chyzdev$sendFeedback(Text message) {
        var player = MinecraftClient.getInstance().player;
        if (player != null) player.sendMessage(message, false);
    }

    @Override
    public ResourceManager chyzdev$getResourceManager() {
        return MinecraftClient.getInstance().getResourceManager();
    }

    @Override
    public SystemDetails chyzdev$getSystemDetails() {
        var client = MinecraftClient.getInstance();
        return MinecraftClientAccessor.chyzdev$addSystemDetailsToCrashReport(
            new SystemDetails(),
            client,
            client.getLanguageManager(),
            client.getGameVersion(),
            client.options
        );
    }
}
