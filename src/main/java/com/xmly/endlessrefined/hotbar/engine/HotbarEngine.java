/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.xmly.endlessrefined.hotbar.engine;

import com.xmly.endlessrefined.compat.Compat;
import com.xmly.endlessrefined.hotbar.HotbarServerState;
import com.xmly.endlessrefined.hotbar.HotbarTable;
import com.xmly.endlessrefined.network.NetworkHandler;
import com.xmly.endlessrefined.network.ServerSupportS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class HotbarEngine {

    private static final String CONFLICTING_MOD = "quadhotbar";

    private HotbarEngine() {
    }

    public static void init() {
        if (Compat.hasQuadHotbar()) {
            throw new IllegalStateException("Endless Refined includes its own fork of QuadHotbar and cannot be used with the " + "standalone '" + CONFLICTING_MOD + "' mod. Remove one of the two.");
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HotbarEngineConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory(HotbarEngineConfigScreen::new)));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HotbarServerState.recover(player);
            HotbarServerState.of(player).sendTable(player);
            NetworkHandler.sendToPlayer(player, new ServerSupportS2C(player.getInventory().selected));
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
