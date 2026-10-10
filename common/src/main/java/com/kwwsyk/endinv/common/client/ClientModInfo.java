package com.kwwsyk.endinv.common.client;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.gui.EndInvSettingScreen;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.network.payloads.toServer.OpenEndInvPayload;

import net.minecraft.client.gui.screens.Screen;

public class ClientModInfo {

    private static IClientConfig clientConfig;

    public static IInputHandler inputHandler;

    public static IContainerScreenHelper containerScreenHelper;

    private static java.util.function.Function<Screen, Screen> configScreenFactory;

    public static IClientConfig getClientConfig() {
        return clientConfig;
    }

    public static void setClientConfig(IClientConfig clientConfig) {
        if(ClientModInfo.clientConfig!=null) throw new IllegalStateException("Try to set config when config has been initialized.");
        ClientModInfo.clientConfig = clientConfig;
    }

    public static void sendOpenMenu(){
        CachedConfig.readAndSyncClientConfigToServer(true);
        ModInfo.getPacketDistributor().sendToServer(new OpenEndInvPayload(true,CachedConfig.currentLayout().rows()));
    }

    public static void setConfigScreenFactory(java.util.function.Function<Screen, Screen> factory) {
        configScreenFactory = factory;
    }

    public static Screen createConfigScreen(Screen parent) {
        if (configScreenFactory != null) {
            Screen screen = configScreenFactory.apply(parent);
            if (screen != null) {
                return screen;
            }
        }
        return new EndInvSettingScreen(parent);
    }
}
