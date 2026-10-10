package com.kwwsyk.endinv.forge.mixin.hotbar;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.forge.hotbar.HotbarServerState;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**When a shift-click in the player's own inventory has nowhere to put the stack, it goes to
 * Endless Inventory instead of staying where it was.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryQuickMoveMixin {

    @Unique
    private int endinv$index = -1;

    @Unique
    private ItemStack endinv$before = ItemStack.EMPTY;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void endinv$captureSlot(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        NonNullList<Slot> slots = ((AbstractContainerMenu) (Object) this).slots;

        endinv$index = index;
        endinv$before = index >= 0 && index < slots.size() ? slots.get(index).getItem().copy() : ItemStack.EMPTY;
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void endinv$overflowToEndless(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!ModInfo.getServerConfig().enableOverflow().get() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NonNullList<Slot> slots = ((AbstractContainerMenu) (Object) this).slots;
        if (endinv$index < 9 || endinv$index > 35 || endinv$index >= slots.size()) {
            return;
        }

        ItemStack now = slots.get(endinv$index).getItem();

        if (now.isEmpty() || !ItemStack.matches(now, endinv$before)) {
            return;
        }

        EndlessInventory endInv = HotbarServerState.endInv(serverPlayer);
        if (endInv == null) {
            return;
        }
        ItemStack remainder = endInv.addItem(ItemKey.asKey(now), now.getCount());
        int stored = now.getCount() - remainder.getCount();

        if (stored <= 0) {
            return;
        }

        slots.get(endinv$index).set(remainder);
        cir.setReturnValue(ItemStack.EMPTY);
    }
}
