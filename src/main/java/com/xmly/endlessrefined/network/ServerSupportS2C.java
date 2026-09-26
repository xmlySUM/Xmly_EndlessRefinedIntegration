/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.xmly.endlessrefined.network;

import com.xmly.endlessrefined.hotbar.engine.HotbarEngineClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerSupportS2C(int selectedSlot) {

    public static void encode(ServerSupportS2C message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.selectedSlot());
    }

    public static ServerSupportS2C decode(FriendlyByteBuf buffer) {
        return new ServerSupportS2C(buffer.readVarInt());
    }

    public static void handle(ServerSupportS2C message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> HotbarEngineClientEvents.syncServerSelectedSlot(message.selectedSlot())));

        context.setPacketHandled(true);
    }
}
