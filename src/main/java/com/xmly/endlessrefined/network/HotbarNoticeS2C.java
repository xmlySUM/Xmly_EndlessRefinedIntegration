package com.xmly.endlessrefined.network;

import com.xmly.endlessrefined.hotbar.engine.HotbarEngineClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HotbarNoticeS2C(String translationKey) {

    public static void encode(HotbarNoticeS2C message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.translationKey());
    }

    public static HotbarNoticeS2C decode(FriendlyByteBuf buffer) {
        return new HotbarNoticeS2C(buffer.readUtf());
    }

    public static void handle(HotbarNoticeS2C message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HotbarEngineClientEvents.showNotice(message.translationKey())));
        context.setPacketHandled(true);
    }
}
