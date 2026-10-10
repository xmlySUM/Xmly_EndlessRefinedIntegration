/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.kwwsyk.endinv.forge.mixin.hotbar.engine;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Redirect(method = "handleSetCarriedItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectionSize()I"))
    private int endinv$allowFullInventorySelection() {
        return Inventory.INVENTORY_SIZE;
    }
}
