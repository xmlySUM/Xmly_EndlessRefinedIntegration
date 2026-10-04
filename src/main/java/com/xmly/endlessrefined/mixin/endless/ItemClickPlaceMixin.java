package com.xmly.endlessrefined.mixin.endless;

import com.kwwsyk.endinv.common.network.payloads.ModPacketContext;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemClickPayload;
import com.xmly.endlessrefined.hotbar.HotbarServerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemClickPayload.class)
public abstract class ItemClickPlaceMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void eri$handlePanelClick(ModPacketContext context, CallbackInfo ci) {
        ItemClickPayload payload = (ItemClickPayload) (Object) this;

        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        HotbarServerState state = HotbarServerState.of(player);
        if (payload.clickType() == ClickType.QUICK_MOVE && !payload.key().isEmpty() && state.isPanelExpanded()) {
            state.placeOnHotbar(player, payload.key());
            ci.cancel();
            return;
        }

        if (payload.clickType() == ClickType.PICKUP && !player.containerMenu.getCarried().isEmpty()) {
            state.onReturnedToEndless(player, player.containerMenu.getCarried());
        }
    }
}
