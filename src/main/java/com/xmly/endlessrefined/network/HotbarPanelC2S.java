package com.xmly.endlessrefined.network;

import com.xmly.endlessrefined.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HotbarPanelC2S(boolean expanded) {

    public static void encode(HotbarPanelC2S message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.expanded());
    }

    public static HotbarPanelC2S decode(FriendlyByteBuf buffer) {
        return new HotbarPanelC2S(buffer.readBoolean());
    }

    public static void handle(HotbarPanelC2S message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                HotbarServerState.of(player).setPanelExpanded(message.expanded());
            }
        });

        context.setPacketHandled(true);
    }
}
