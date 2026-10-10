package com.kwwsyk.endinv.forge.integrates.clothconfig;

import com.kwwsyk.endinv.common.client.ClientModInfo;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

public final class ClothConfigIntegration {

    private ClothConfigIntegration() {
    }

    public static void register(ModContainer container) {
        // Registered whether or not Cloth Config is present. Without this the mod list has no
        // Config button at all, so a player without Cloth Config had no way to reach any setting.
        // With Cloth the button opens Cloth's screen, which covers the client and server configs;
        // without it, this mod's own settings screen.
        container.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(ClientModInfo::createConfigScreen)
        );
        if (ModList.get().isLoaded("cloth_config")) {
            ClientModInfo.setConfigScreenFactory(ClothConfigScreenBuilder::create);
        }
    }
}
