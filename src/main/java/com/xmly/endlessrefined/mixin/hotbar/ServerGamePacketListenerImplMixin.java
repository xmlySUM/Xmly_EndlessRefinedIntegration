/*
 * Derived from QuadHotbar by ArchangelD (mod_id: quadhotbar), LGPL-3.0.
 * This file remains under LGPL-3.0. See THIRD-PARTY.md for the list of changes.
 */
package com.xmly.endlessrefined.mixin.hotbar;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets the server accept a carried-slot update beyond slot 8.
 *
 * <p>{@code handleSetCarriedItem} rejects anything outside
 * {@code Inventory#getSelectionSize()}, which is hardcoded to 9. The client sends the
 * extended slot with the ordinary {@code ServerboundSetCarriedItemPacket}, so this is
 * the only thing standing between the engine and a server that ignores it.
 *
 * <p>This is also the injection that makes the two mods mutually exclusive: QuadHotbar
 * redirects the very same call, and mixin refuses to apply two redirects to one
 * instruction.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Redirect(
            method = "handleSetCarriedItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectionSize()I"))
    private int eri$allowFullInventorySelection() {
        return Inventory.INVENTORY_SIZE;
    }
}
