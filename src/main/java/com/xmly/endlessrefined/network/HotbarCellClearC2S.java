package com.xmly.endlessrefined.network;

import com.xmly.endlessrefined.hotbar.HotbarServerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HotbarCellClearC2S(int cell) {

    public static void encode(HotbarCellClearC2S message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.cell());
    }

    public static HotbarCellClearC2S decode(FriendlyByteBuf buffer) {
        return new HotbarCellClearC2S(buffer.readVarInt());
    }

    public static void handle(HotbarCellClearC2S message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                HotbarServerState.of(player).clearCell(player, message.cell());
            }
        });

        context.setPacketHandled(true);
    }
}
