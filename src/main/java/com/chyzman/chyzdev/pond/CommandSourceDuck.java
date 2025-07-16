package com.chyzman.chyzdev.pond;

import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.SystemDetails;

public interface CommandSourceDuck {
    void chyzdev$sendFeedback(Text message);

    ResourceManager chyzdev$getResourceManager();

    SystemDetails chyzdev$getSystemDetails();
}
