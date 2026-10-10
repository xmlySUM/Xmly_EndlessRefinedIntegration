package com.kwwsyk.endinv.forge.network.payloads;

import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**Whether the player has the nine by nine hotbar panel open.<br>
 * The server needs to know because a panel that is open changes what a quick-move on the EndInv
 * page means: it puts the item on the hotbar rather than into the container.
 */
public record HotbarPanelPayload(boolean expanded) implements ModPacketPayload {

    public static void encode(HotbarPanelPayload message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.expanded());
    }

    public static HotbarPanelPayload decode(FriendlyByteBuf buffer) {
        return new HotbarPanelPayload(buffer.readBoolean());
    }

    @Override
    public String id() {
        return "hotbar_panel";
    }

    @Override
    public void handle(ModPacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            HotbarServerState.of(player).setPanelExpanded(expanded);
        }
    }
}
