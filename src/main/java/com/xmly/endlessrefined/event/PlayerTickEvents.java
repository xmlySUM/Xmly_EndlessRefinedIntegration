package com.xmly.endlessrefined.event;

import com.xmly.endlessrefined.hotbar.HotbarServerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PlayerTickEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (event.player instanceof ServerPlayer player) {
            HotbarServerState.tick(player);
        }
    }
}
