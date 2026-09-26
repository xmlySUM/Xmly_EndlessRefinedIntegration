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

/**
 * Sets up the vendored hotbar engine, and refuses to run alongside the mod it came from.
 */
@SuppressWarnings("removal")
public final class HotbarEngine {

    private static final String CONFLICTING_MOD = "quadhotbar";

    private HotbarEngine() {
    }

    public static void init() {
        if (Compat.hasQuadHotbar()) {
            // Not a defensive check for safety's sake: coexistence is impossible. Both
            // this mod and QuadHotbar must redirect the same Inventory#getSelectionSize()
            // call in ServerGamePacketListenerImpl#handleSetCarriedItem, and two
            // @Redirects on one instruction abort mixin application. Failing here with a
            // readable message beats the cryptic injection error that would otherwise
            // appear.
            throw new IllegalStateException(
                    "Endless Refined includes its own fork of QuadHotbar and cannot be used with the "
                            + "standalone '" + CONFLICTING_MOD + "' mod. Remove one of the two.");
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HotbarEngineConfig.SPEC);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(HotbarEngineConfigScreen::new)));
    }

    /**
     * Vanilla does not tell the client which slot the server thinks is selected, so a
     * client connecting to a server that had already saved a selection above 8 would
     * otherwise start out of sync.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HotbarServerState.recover(player);

            // The panel draws the table, and it can be opened before any page key is
            // pressed, so the client needs it from the start.
            HotbarServerState.of(player).sendTable(player);

            NetworkHandler.sendToPlayer(player, new ServerSupportS2C(player.getInventory().selected));
        }
    }

    /**
     * Drops the per-session state. Nothing on the player needs undoing: stacks that were
     * checked out are their items, and the stock was reduced when they were drawn.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Stowed first: a swapped-in ender chest belongs in the ender chest, not in whatever
            // the player's inventory gets saved as.
            HotbarServerState.stow(player);
            HotbarServerState.forget(player);
        }
    }

    /**
     * Carries the hotbar layout across a respawn.
     *
     * <p>Forge does not copy persistent data onto the new player entity, so without this
     * every death would cost the player their hotbar layout.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HotbarTable.copy(event.getOriginal().getPersistentData(), player.getPersistentData());
        }
    }
}
