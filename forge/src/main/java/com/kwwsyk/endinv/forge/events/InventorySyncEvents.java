package com.kwwsyk.endinv.forge.events;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.data.EndlessInventoryData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**Keeps the screens that are open on an inventory in step with it.<br>
 * Anything that changes an inventory from outside the screen looking at it - a Refined Storage
 * network, another player, a command, the player's own hotbar - would otherwise leave that screen
 * showing what used to be there until it asked again.
 */
@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public final class InventorySyncEvents {

    private InventorySyncEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EndlessInventoryData data = ServerLevelEndInv.levelEndInvData;
        if (data == null) {
            return;
        }
        for (EndlessInventory endInv : data.levelEndInvs) {
            endInv.broadcastChanges();
        }
    }
}
