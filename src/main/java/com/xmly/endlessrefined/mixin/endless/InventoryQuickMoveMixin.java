package com.xmly.endlessrefined.mixin.endless;

import com.xmly.endlessrefined.config.ERIConfig;
import com.xmly.endlessrefined.endless.EndlessBridge;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryQuickMoveMixin {

    @Unique
    private int eri$index = -1;

    @Unique
    private ItemStack eri$before = ItemStack.EMPTY;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void eri$captureSlot(Player pPlayer, int pIndex, CallbackInfoReturnable<ItemStack> cir) {
        NonNullList<Slot> slots = ((ContainerMenuAccessor) this).eri$slots();

        eri$index = pIndex;
        eri$before = pIndex >= 0 && pIndex < slots.size() ? slots.get(pIndex).getItem().copy() : ItemStack.EMPTY;
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void eri$overflowToEndless(Player pPlayer, int pIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (!ERIConfig.ENABLE_OVERFLOW.get() || !(pPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NonNullList<Slot> slots = ((ContainerMenuAccessor) this).eri$slots();

        // Only the main inventory: the hotbar slots have nowhere further to go, and the
        // other slots are armour and crafting.
        if (eri$index < 9 || eri$index > 35 || eri$index >= slots.size()) {
            return;
        }

        ItemStack now = slots.get(eri$index).getItem();

        if (now.isEmpty() || !ItemStack.matches(now, eri$before)) {
            return;
        }

        ItemStack remainder = EndlessBridge.insert(serverPlayer, now);
        int stored = now.getCount() - remainder.getCount();

        if (stored <= 0) {
            return;
        }

        slots.get(eri$index).set(remainder);
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
