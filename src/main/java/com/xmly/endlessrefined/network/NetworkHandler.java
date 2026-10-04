package com.xmly.endlessrefined.network;

import com.xmly.endlessrefined.EndlessRefined;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = EndlessRefined.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(EndlessRefined.MOD_ID, "main")).clientAcceptedVersions(PROTOCOL_VERSION::equals).serverAcceptedVersions(PROTOCOL_VERSION::equals).networkProtocolVersion(() -> PROTOCOL_VERSION).simpleChannel();

    private NetworkHandler() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::registerMessages);
    }

    private static void registerMessages() {
        int id = 0;
        CHANNEL.registerMessage(id++, ServerSupportS2C.class, ServerSupportS2C::encode, ServerSupportS2C::decode, ServerSupportS2C::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, HotbarNoticeS2C.class, HotbarNoticeS2C::encode, HotbarNoticeS2C::decode, HotbarNoticeS2C::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, HotbarStateC2S.class, HotbarStateC2S::encode, HotbarStateC2S::decode, HotbarStateC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, HotbarPanelC2S.class, HotbarPanelC2S::encode, HotbarPanelC2S::decode, HotbarPanelC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ScreenOpenC2S.class, ScreenOpenC2S::encode, ScreenOpenC2S::decode, ScreenOpenC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, HotbarCellClearC2S.class, HotbarCellClearC2S::encode, HotbarCellClearC2S::decode, HotbarCellClearC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, HotbarCellPlaceC2S.class, HotbarCellPlaceC2S::encode, HotbarCellPlaceC2S::decode, HotbarCellPlaceC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, HotbarTableS2C.class, HotbarTableS2C::encode, HotbarTableS2C::decode, HotbarTableS2C::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static <MSG> void sendToServer(MSG message) {
        if (!Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientConnection::isPresent))) {
            return;
        }
        CHANNEL.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG message) {
        if (player instanceof FakePlayer) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
