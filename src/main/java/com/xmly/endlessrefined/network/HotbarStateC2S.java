package com.xmly.endlessrefined.network;

import com.xmly.endlessrefined.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public record HotbarStateC2S(boolean pageKey, int digit, int groups) {

    public static void encode(HotbarStateC2S message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.pageKey());
        buffer.writeVarInt(message.digit());
        buffer.writeVarInt(message.groups());
    }

    public static HotbarStateC2S decode(FriendlyByteBuf buffer) {
        return new HotbarStateC2S(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(HotbarStateC2S message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                HotbarServerState.of(player).apply(player, message.digit(), message.groups(), message.pageKey());
            }
        });
        context.setPacketHandled(true);
    }
}
