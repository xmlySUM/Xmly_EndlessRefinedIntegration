package com.kwwsyk.endinv.forge.mixin.refinedstorage;

import com.kwwsyk.endinv.common.client.option.MenuAttachabilityCache;
import com.refinedmods.refinedstorage.container.GridContainerMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**Keeps the EndInv panel off a Refined Storage grid screen.<br>
 * The grid already shows the same contents, so the panel would be a second view of the same
 * inventory competing for the same clicks.
 */
@Mixin(MenuAttachabilityCache.class)
public abstract class NoAttachOnGridMixin {

    @Inject(method = "isAttachable", at = @At("RETURN"), cancellable = true, remap = false)
    private static void endinv$skipRefinedStorageGrid(AbstractContainerScreen<?> screen, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (screen.getMenu() instanceof GridContainerMenu) {
            cir.setReturnValue(false);
        }
    }
}
