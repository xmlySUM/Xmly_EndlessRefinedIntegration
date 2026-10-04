package com.xmly.endlessrefined.network;

import net.minecraft.client.Minecraft;

final class ClientConnection {

    private ClientConnection() {
    }

    static boolean isPresent() {
        return Minecraft.getInstance().getConnection() != null;
    }
}
