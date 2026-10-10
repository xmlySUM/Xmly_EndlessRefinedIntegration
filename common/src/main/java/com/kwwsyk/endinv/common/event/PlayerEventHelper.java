package com.kwwsyk.endinv.common.event;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvContent;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvMetadata;
import com.kwwsyk.endinv.common.network.payloads.toClient.MenuAttachabilityPayload;
import com.kwwsyk.endinv.common.options.ContentTransferMode;
import net.minecraft.server.level.ServerPlayer;

import static com.kwwsyk.endinv.common.ModInfo.getPacketDistributor;
import static com.kwwsyk.endinv.common.ModInfo.getServerConfig;

public class PlayerEventHelper {

    public static void sendData(ServerPlayer serverPlayer){
        getPacketDistributor().sendToAllPlayer(ModRegistries.NbtAttachments.getSyncedConfig().computeIfAbsent(serverPlayer));
        // Send effective menu attachability
        var serverCfg = ModInfo.getServerConfig();
        var menuCfg = serverCfg.specifiedMenuAttachability().get();
        boolean defaultAttach = serverCfg.enableAttaching().get();
        getPacketDistributor().sendToAllPlayer(
                new MenuAttachabilityPayload(
                        defaultAttach,
                        menuCfg.isInventoryAttachable(),
                        menuCfg.getConfigs()
                ));
        if(getServerConfig().transferMode().get()== ContentTransferMode.ALL){
            ServerLevelEndInv.getEndInvForPlayer(serverPlayer).ifPresent(endInv -> {
                getPacketDistributor().sendToAllPlayer(new EndInvContent(endInv.snapshotItemMap()));
                getPacketDistributor().sendToAllPlayer(EndInvMetadata.getWith(endInv));
            });
        }
    }
}
