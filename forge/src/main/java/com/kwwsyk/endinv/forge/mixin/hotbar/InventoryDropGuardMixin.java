package com.kwwsyk.endinv.forge.mixin.hotbar;

import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class InventoryDropGuardMixin {

    @Shadow
    @Final
    public Player player;

    /**The ender chest's contents are physically in the player's inventory while it is swapped into
     * the hotbar, so they have to go back before a death scatters them.
     */
    @Inject(method = "dropAll", at = @At("HEAD"))
    private void endinv$stowEnderChestBeforeDrop(CallbackInfo ci) {
        if (this.player instanceof ServerPlayer serverPlayer) {
            HotbarServerState.stow(serverPlayer);
        }
    }
}
