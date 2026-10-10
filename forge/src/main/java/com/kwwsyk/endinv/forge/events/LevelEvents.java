package com.kwwsyk.endinv.forge.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.data.EndlessInventoryData;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public class LevelEvents {

    @SubscribeEvent
    public static void load(LevelEvent.Load event){
        PlayerEvents.tickRefresh = true;
        if(event.getLevel() instanceof ServerLevel serverLevel){
            EndlessInventoryData.init(serverLevel);
        }

    }
}
