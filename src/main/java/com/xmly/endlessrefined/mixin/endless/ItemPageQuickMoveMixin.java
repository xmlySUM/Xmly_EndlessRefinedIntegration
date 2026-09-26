package com.xmly.endlessrefined.mixin.endless;

import com.kwwsyk.endinv.common.client.gui.page.ItemPage;
import com.xmly.endlessrefined.client.HotbarPanelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemPage.class)
public abstract class ItemPageQuickMoveMixin {

    @Inject(method = "handleQuickMove", at = @At("HEAD"), cancellable = true, remap = false)
    private void eri$leaveItToTheServer(ItemPage.ItemPointer clicked, CallbackInfo ci) {
        if (HotbarPanelState.isExpanded()) {
            ci.cancel();
        }
    }
}
