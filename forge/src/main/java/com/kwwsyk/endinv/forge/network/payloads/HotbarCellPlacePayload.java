package com.kwwsyk.endinv.forge.network.payloads;

import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**The player asked for an item to be put on the hotbar from the EndInv page. */
public record HotbarCellPlacePayload(ItemKey key) implements ModPacketPayload {

    public static void encode(HotbarCellPlacePayload message, FriendlyByteBuf buffer) {
        ItemKey.encode(buffer, message.key());
    }

    public static HotbarCellPlacePayload decode(FriendlyByteBuf buffer) {
        return new HotbarCellPlacePayload(ItemKey.decode(buffer));
    }

    @Override
    public String id() {
        return "hotbar_cell_place";
    }

    @Override
    public void handle(ModPacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            HotbarServerState.of(player).placeOnHotbar(player, key);
        }
    }
}
