package com.kwwsyk.endinv.forge.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.event.PlayerEventHelper;
import com.kwwsyk.endinv.forge.nbtAttcachment.AttachingCapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public class PlayerEvents {

    public static boolean tickRefresh =true;
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer){
            if(tickRefresh) {
                PlayerEventHelper.sendData(serverPlayer);
                tickRefresh = false ;
            }
        }
    }

    @SubscribeEvent
    public static void onRespawnClone(PlayerEvent.Clone event){//serverOnly determined by forge's internal mechanics.
        tickRefresh=true;

        if (!event.isWasDeath()) {
            return; // 如果你只在死亡重生时处理
        }

        Player oldPlayer = event.getOriginal();
        // 新玩家
        Player newPlayer = event.getEntity();

        // 只在重生时保留（死亡或传送切维度时可根据需求调整）


        // 复制 EndInvUuid
            //getEndInvUUID().setTo(newPlayer, getEndInvUUID().getWith(oldPlayer));
        // 复制 ISyncedConfigImpl
            //getSyncedConfig().setTo(newPlayer, getSyncedConfig().getWith(oldPlayer));
        try {
            oldPlayer.reviveCaps();
            // 复制 EndInvUuid
            oldPlayer.getCapability(AttachingCapabilities.END_INV_UUID).ifPresent(oldCap -> {
                newPlayer.getCapability(AttachingCapabilities.END_INV_UUID).ifPresent(newCap -> {
                    newCap.setUuid(oldCap.getUuid());
                });
            });

            // 复制 ISyncedConfigImpl
            oldPlayer.getCapability(AttachingCapabilities.END_INV_CONFIG).ifPresent(oldCap -> {
                newPlayer.getCapability(AttachingCapabilities.END_INV_CONFIG).ifPresent(newCap -> {
                    newCap.setSyncedConfig(oldCap.getSyncedConfig());
                    ModInfo.getPacketDistributor().sendToPlayer((ServerPlayer) newPlayer, oldCap.getSyncedConfig());
                });
            });
        }finally {
            oldPlayer.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(PlayerEvent.PlayerLoggedInEvent event){
        tickRefresh = true;
    }
}
