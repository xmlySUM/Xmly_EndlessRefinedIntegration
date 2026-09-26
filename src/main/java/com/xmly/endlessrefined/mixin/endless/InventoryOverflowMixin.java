package com.xmly.endlessrefined.mixin.endless;

import com.xmly.endlessrefined.config.ERIConfig;
import com.xmly.endlessrefined.endless.EndlessBridge;
import com.xmly.endlessrefined.inventory.InventoryTransfer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryOverflowMixin {

    @Shadow
    @Final
    public Player player;

    @Unique
    private ItemStack eri$offered = ItemStack.EMPTY;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void eri$captureOffered(ItemStack pStack, CallbackInfoReturnable<Boolean> cir) {
        if (this.player instanceof ServerPlayer) {
            eri$offered = pStack.copy();
        }
    }

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void eri$endlessOverflow(ItemStack pStack, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.player instanceof ServerPlayer serverPlayer) || this.player instanceof FakePlayer) {
            return;
        }

        ItemStack offered = eri$offered;
        eri$offered = ItemStack.EMPTY;

        if (offered.isEmpty()) {
            return;
        }

        if (cir.getReturnValueZ()) {
            if (!ERIConfig.ENABLE_SWEEP.get()) {
                return;
            }

            int added = offered.getCount() - pStack.getCount();

            if (added > 0) {
                InventoryTransfer.moveExtendedToEndless(serverPlayer, offered, added);
            }

            return;
        }

        if (!ERIConfig.ENABLE_OVERFLOW.get() || pStack.isEmpty()) {
            return;
        }

        ItemStack remainder = EndlessBridge.insert(serverPlayer, pStack);
        int stored = pStack.getCount() - remainder.getCount();

        if (stored <= 0) {
            return;
        }

        pStack.shrink(stored);

        cir.setReturnValue(true);
    }
}
