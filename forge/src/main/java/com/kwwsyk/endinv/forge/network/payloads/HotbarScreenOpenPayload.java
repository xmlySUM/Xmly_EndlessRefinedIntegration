package com.kwwsyk.endinv.forge.network.payloads;

import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**Whether the player has a container screen open.<br>
 * Rows are only refilled from Endless Inventory while no screen is open, so that a row never
 * changes under the player's hands.
 */
public record HotbarScreenOpenPayload(boolean open) implements ModPacketPayload {

    public static void encode(HotbarScreenOpenPayload message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.open());
    }

    public static HotbarScreenOpenPayload decode(FriendlyByteBuf buffer) {
        return new HotbarScreenOpenPayload(buffer.readBoolean());
    }

    @Override
    public String id() {
        return "hotbar_screen_open";
    }

    @Override
    public void handle(ModPacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            HotbarServerState.of(player).setScreenOpen(open);
        }
    }
}
