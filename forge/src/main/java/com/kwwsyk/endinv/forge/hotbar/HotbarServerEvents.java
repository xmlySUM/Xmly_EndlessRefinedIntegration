package com.kwwsyk.endinv.forge.hotbar;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.hotbar.HotbarTable;
import com.kwwsyk.endinv.forge.network.payloads.ServerSupportPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**Keeps every player's hotbar state moving on the server.<br>
 * The ender chest handling runs whether or not the feature is enabled: if it was on when the
 * player left, their ender chest contents are physically in their inventory and have to go back
 * even if an operator has since turned the feature off.
 */
@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public final class HotbarServerEvents {

    private HotbarServerEvents() {
    }

    private static boolean enabled() {
        return ModInfo.getServerConfig().enableHotbar().get();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled()) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            HotbarServerState.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean enabled = enabled();
        ModInfo.getPacketDistributor().sendToPlayer(player,
                new ServerSupportPayload(player.getInventory().selected, enabled));
        //Putting the ender chest back has to happen even when the feature is off.
        HotbarServerState.recover(player);
        if (enabled) {
            HotbarServerState.of(player).sendTable(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HotbarServerState.stow(player);
            HotbarServerState.forget(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HotbarTable.copy(event.getOriginal().getPersistentData(), player.getPersistentData());
        }
    }
}
