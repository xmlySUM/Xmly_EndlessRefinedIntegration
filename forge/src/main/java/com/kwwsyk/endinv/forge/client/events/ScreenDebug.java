package com.kwwsyk.endinv.forge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.forge.client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID,value = Dist.CLIENT)
public class ScreenDebug {

    public static int phase = 0;
    private static boolean hideMenu = false;

    @SubscribeEvent
    public static void hideScreen(ScreenEvent.Render.Pre event){
        if(hideMenu){
            GuiGraphics graphics = event.getGuiGraphics();
            Screen screen = event.getScreen();
            Minecraft mc = screen.getMinecraft();
            graphics.drawString(mc.font,Component.literal("[F4]Menu screen rendering stopped!"),0,0,0xFFFFAA00);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderScreen(ScreenEvent.Render.Post event){
        if(!ClientConfig.CONFIG.ENABLE_DEBUG.get()) return;
        Screen screen = event.getScreen();
        com.kwwsyk.endinv.common.client.ScreenDebug.debugInfo(screen,event.getGuiGraphics(),event.getMouseX(),event.getMouseY());
    }

    @SubscribeEvent
    public static void click(ScreenEvent.KeyPressed.Post event){
        com.kwwsyk.endinv.common.client.ScreenDebug.click(event.getKeyCode(),event.getScreen());
    }
}
