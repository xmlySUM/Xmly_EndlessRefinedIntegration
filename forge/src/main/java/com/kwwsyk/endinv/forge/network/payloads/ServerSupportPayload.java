package com.kwwsyk.endinv.forge.network.payloads;

import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.forge.client.hotbar.HotbarEngineClientEvents;
import net.minecraft.network.FriendlyByteBuf;

/**What the server allows the client's hotbar to do, sent on join.<br>
 * The client has to be told rather than reading the server's config: a client connected to a
 * dedicated server never sees that server's server-side config file, so {@code hotbar.enabled}
 * would silently read as its local default. Without this flag the client would draw a multi-group
 * hotbar that the server then refuses to move.
 */
public record ServerSupportPayload(int selectedSlot, boolean hotbarEnabled) implements ModPacketPayload {

    public static void encode(ServerSupportPayload message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.selectedSlot());
        buffer.writeBoolean(message.hotbarEnabled());
    }

    public static ServerSupportPayload decode(FriendlyByteBuf buffer) {
        return new ServerSupportPayload(buffer.readVarInt(), buffer.readBoolean());
    }

    @Override
    public String id() {
        return "hotbar_server_support";
    }

    @Override
    public void handle(ModPacketContext context) {
        HotbarEngineClientEvents.syncServerSupport(selectedSlot, hotbarEnabled);
    }
}
