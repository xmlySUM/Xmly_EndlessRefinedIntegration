package com.kwwsyk.endinv.forge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ModRegistries;
import com.kwwsyk.endinv.common.client.gui.EndlessInventoryScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD,modid = ModInfo.MOD_ID,value = Dist.CLIENT)
public class MenuScreenReg {
    @SubscribeEvent
    public static void reg(FMLClientSetupEvent event){
        event.enqueueWork(
                ()-> MenuScreens.register(ModRegistries.Menus.getEndInvMenuType(),EndlessInventoryScreen::new)
        );
    }
}
