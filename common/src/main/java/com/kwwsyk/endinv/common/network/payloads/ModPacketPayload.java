package com.kwwsyk.endinv.common.network.payloads;

import com.kwwsyk.endinv.common.client.gui.ScreenFramework;

import java.util.Optional;

public interface ModPacketPayload{

    String id();

    void handle(ModPacketContext context);

    static Optional<com.kwwsyk.endinv.common.client.gui.page.manager.PageManager> getClientPageMeta(){
        return Optional.ofNullable(ScreenFramework.getInstance());
    }
}
