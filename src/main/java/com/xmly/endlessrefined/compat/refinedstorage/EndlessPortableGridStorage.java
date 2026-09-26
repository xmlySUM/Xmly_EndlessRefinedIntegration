package com.xmly.endlessrefined.compat.refinedstorage;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.refinedmods.refinedstorage.api.storage.AccessType;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDiskContainerContext;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDiskListener;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.blockentity.grid.portable.PortableGrid;
import com.xmly.endlessrefined.EndlessRefined;
import com.xmly.endlessrefined.endless.EndlessBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


public final class EndlessPortableGridStorage implements IStorageDisk<ItemStack> {

    @SuppressWarnings("removal")
    private static final ResourceLocation FACTORY_ID = new ResourceLocation(EndlessRefined.MOD_ID, "endless_portable_grid");

    private final PortableGrid grid;

    public EndlessPortableGridStorage(PortableGrid grid) {
        this.grid = grid;
    }

    @Nullable
    private EndlessInventory endInv() {
        return EndlessBridge.forPlayer(grid.getPlayer());
    }

    @Override
    public Collection<ItemStack> getStacks() {
        EndlessInventory endInv = endInv();

        return endInv == null ? List.of() : endInv.getItemsAsList();
    }

    @Override
    @Nonnull
    public ItemStack insert(@Nonnull ItemStack stack, int size, Action action) {
        if (stack.isEmpty() || size <= 0) {
            return ItemStack.EMPTY;
        }

        EndlessInventory endInv = endInv();

        if (endInv == null) {
            return stack.copyWithCount(size);
        }

        ItemKey key = ItemKey.asKey(stack);

        if (action == Action.SIMULATE) {
            int accepted = EndlessBridge.acceptedCount(endInv, key, size);

            return accepted >= size ? ItemStack.EMPTY : stack.copyWithCount(size - accepted);
        }

        ItemStack remainder = endInv.addItem(key, size);
        int inserted = size - remainder.getCount();

        if (inserted > 0) {
            IStorageCache<ItemStack> cache = grid.getItemCache();

            if (cache != null) {
                cache.add(stack, inserted, false, false);
            }
        }

        return remainder;
    }

    @Override
    @Nonnull
    public ItemStack extract(@Nonnull ItemStack stack, int size, int flags, Action action) {
        if (stack.isEmpty() || size <= 0) {
            return ItemStack.EMPTY;
        }

        EndlessInventory endInv = endInv();

        if (endInv == null) {
            return ItemStack.EMPTY;
        }

        ItemKey key = ItemKey.asKey(stack);
        int available = EndlessBridge.availableCount(endInv, key);

        if (available <= 0) {
            return ItemStack.EMPTY;
        }

        // In infinity mode the pool never drains, so every request succeeds in full.
        boolean infinite = endInv.isInfinityMode() && available >= endInv.getMaxItemStackSize();
        int amount = infinite ? size : Math.min(size, available);

        if (action == Action.SIMULATE) {
            return EndlessBridge.toStack(key, amount);
        }

        ItemStack taken = endInv.takeItem(key, amount);

        if (taken.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // takeItem hands back the whole pool in infinity mode; the caller only asked for `amount`.
        if (taken.getCount() > amount) {
            taken.setCount(amount);
        }

        if (!infinite) {
            IStorageCache<ItemStack> cache = grid.getItemCache();

            if (cache != null) {
                cache.remove(taken, taken.getCount(), false);
            }
        }

        return taken;
    }

    @Override
    public int getStored() {
        EndlessInventory endInv = endInv();

        return endInv == null ? 0 : EndlessBridge.storedTotal(endInv);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.INSERT_EXTRACT;
    }

    @Override
    public int getCacheDelta(int storedPreInsertion, int size, @Nullable ItemStack remainder) {
        return size - (remainder == null || remainder.isEmpty() ? 0 : remainder.getCount());
    }

    /**
     * @return {@link Integer#MAX_VALUE}: an Endless Inventory has no meaningful cap
     */
    @Override
    public int getCapacity() {
        return Integer.MAX_VALUE;
    }

    @Nullable
    @Override
    public UUID getOwner() {
        Player player = grid.getPlayer();

        return player == null ? null : player.getUUID();
    }

    @Override
    public void setSettings(@Nullable IStorageDiskListener listener, IStorageDiskContainerContext context) {
        // No settings to adopt: this storage is not a real disk.
    }

    @Override
    public CompoundTag writeToNbt() {
        // Never persisted. The Endless Inventory owns the data and saves itself.
        return new CompoundTag();
    }

    @Override
    public ResourceLocation getFactoryId() {
        return FACTORY_ID;
    }
}
