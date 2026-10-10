package com.kwwsyk.endinv.forge.network.payloads;

import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**The player pressed Ctrl+digit, or told the server how many hotbar groups they configured. */
public record HotbarStatePayload(boolean pageKey, int digit, int groups) implements ModPacketPayload {

    public static void encode(HotbarStatePayload message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.pageKey());
        buffer.writeVarInt(message.digit());
        buffer.writeVarInt(message.groups());
    }

    public static HotbarStatePayload decode(FriendlyByteBuf buffer) {
        return new HotbarStatePayload(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public String id() {
        return "hotbar_state";
    }

    @Override
    public void handle(ModPacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            HotbarServerState.of(player).apply(player, digit, groups, pageKey);
        }
    }
}
