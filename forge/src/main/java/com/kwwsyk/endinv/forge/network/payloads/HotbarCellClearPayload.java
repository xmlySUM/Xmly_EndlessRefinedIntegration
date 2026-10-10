package com.kwwsyk.endinv.forge.network.payloads;

import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**The player shift-clicked a cell of the hotbar panel to take that item off the hotbar. */
public record HotbarCellClearPayload(int cell) implements ModPacketPayload {

    public static void encode(HotbarCellClearPayload message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.cell());
    }

    public static HotbarCellClearPayload decode(FriendlyByteBuf buffer) {
        return new HotbarCellClearPayload(buffer.readVarInt());
    }

    @Override
    public String id() {
        return "hotbar_cell_clear";
    }

    @Override
    public void handle(ModPacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            HotbarServerState.of(player).clearCell(player, cell);
        }
    }
}
