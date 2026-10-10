package com.kwwsyk.endinv.common.network.payloads.toServer;

import com.kwwsyk.endinv.common.menu.EndlessInventoryMenu;
import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.ModPacketPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record ToggleCraftingPayload(boolean visible) implements ModPacketPayload {


    public static void encode(ToggleCraftingPayload payload, FriendlyByteBuf buf) {
        buf.writeBoolean(payload.visible);
    }

    public static ToggleCraftingPayload decode(FriendlyByteBuf buf) {
        return new ToggleCraftingPayload(buf.readBoolean());
    }

    @Override
    public String id() {
        return "toggle_crafting";
    }

    @Override
    public void handle(ModPacketContext context) {
        Player player = context.player();
        if (player == null) {
            return;
        }
        if (player.containerMenu instanceof EndlessInventoryMenu menu) {
            menu.setCraftingVisible(visible);
            menu.broadcastChanges();
        }
    }
}



