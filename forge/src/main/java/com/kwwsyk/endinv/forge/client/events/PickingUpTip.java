package com.kwwsyk.endinv.forge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.event.AutoPickTipper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT,modid = ModInfo.MOD_ID)
public class PickingUpTip {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        AutoPickTipper.onRenderGui(event.getGuiGraphics());
    }
}
