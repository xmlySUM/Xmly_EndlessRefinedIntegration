package com.xmly.endlessrefined.network;

import com.xmly.endlessrefined.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ScreenOpenC2S(boolean open) {
    public static void encode(ScreenOpenC2S message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.open());
    }

    public static ScreenOpenC2S decode(FriendlyByteBuf buffer) {
        return new ScreenOpenC2S(buffer.readBoolean());
    }

    public static void handle(ScreenOpenC2S message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                HotbarServerState.of(player).setScreenOpen(message.open());
            }
        });
        context.setPacketHandled(true);
    }
}
