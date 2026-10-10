package com.kwwsyk.endinv.forge.hotbar;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.util.ItemKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**Moves items the player passively gained out of the extended inventory slots and into Endless
 * Inventory.<br>
 * Slots the hotbar is currently showing are skipped: those hold real items the player is looking
 * at on their hotbar, and sweeping them away would empty it under their hands.
 */
public final class InventoryTransfer {

    private InventoryTransfer() {
    }

    public static int moveExtendedToEndless(ServerPlayer player, ItemStack prototype, int maxAmount) {
        if (prototype.isEmpty() || maxAmount <= 0) {
            return 0;
        }
        EndlessInventory endInv = HotbarServerState.endInv(player);
        if (endInv == null) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int moved = 0;
        for (int slot = HotbarServerState.EXTENDED_FIRST; slot < HotbarServerState.EXTENDED_LAST && moved < maxAmount; slot++) {
            if (HotbarServerState.ownsSlot(player, slot)) {
                continue;
            }
            ItemStack inSlot = inventory.getItem(slot);
            if (inSlot.isEmpty() || !ItemStack.isSameItemSameTags(inSlot, prototype)) {
                continue;
            }
            int want = Math.min(maxAmount - moved, inSlot.getCount());

            ItemStack remainder = endInv.addItem(ItemKey.asKey(inSlot), want);
            int stored = want - remainder.getCount();
            if (stored <= 0) {
                continue;
            }
            inSlot.shrink(stored);
            if (inSlot.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            moved += stored;
        }
        if (moved > 0) {
            inventory.setChanged();
        }
        return moved;
    }
}
