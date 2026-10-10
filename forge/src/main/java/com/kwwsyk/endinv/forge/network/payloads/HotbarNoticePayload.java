package com.kwwsyk.endinv.forge.network.payloads;

import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.forge.client.hotbar.HotbarEngineClientEvents;
import net.minecraft.network.FriendlyByteBuf;

/**A one line message to show above the hotbar, such as why a page could not be shown. */
public record HotbarNoticePayload(String translationKey) implements ModPacketPayload {

    public static void encode(HotbarNoticePayload message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.translationKey());
    }

    public static HotbarNoticePayload decode(FriendlyByteBuf buffer) {
        return new HotbarNoticePayload(buffer.readUtf());
    }

    @Override
    public String id() {
        return "hotbar_notice";
    }

    @Override
    public void handle(ModPacketContext context) {
        HotbarEngineClientEvents.showNotice(translationKey);
    }
}
