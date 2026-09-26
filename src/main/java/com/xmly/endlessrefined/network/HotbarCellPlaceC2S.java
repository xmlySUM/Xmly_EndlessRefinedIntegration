package com.xmly.endlessrefined.network;

import com.kwwsyk.endinv.common.util.ItemKey;
import com.xmly.endlessrefined.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HotbarCellPlaceC2S(ItemKey key) {

    public static void encode(HotbarCellPlaceC2S message, FriendlyByteBuf buffer) {
        KeyCodec.write(buffer, message.key());
    }

    public static HotbarCellPlaceC2S decode(FriendlyByteBuf buffer) {
        return new HotbarCellPlaceC2S(KeyCodec.read(buffer));
    }

    public static void handle(HotbarCellPlaceC2S message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                HotbarServerState.of(player).placeOnHotbar(player, message.key());
            }
        });

        context.setPacketHandled(true);
    }
}
