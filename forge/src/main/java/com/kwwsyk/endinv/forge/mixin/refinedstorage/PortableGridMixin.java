package com.kwwsyk.endinv.forge.mixin.refinedstorage;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.forge.integrates.refinedstorage.RefinedStorageCompat;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.blockentity.grid.portable.PortableGrid;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**Gives a portable grid with no disk something to fall back on: the player's Endless Inventory.
 * Every hook only takes effect when the grid has no disk and the integration is on, so a grid
 * with a disk behaves exactly as it did.
 */
@Mixin(PortableGrid.class)
@SuppressWarnings({"rawtypes"})
public abstract class PortableGridMixin {
    @Unique
    @Nullable
    private IStorageDisk<ItemStack> endinv$storage;
    @Unique
    @Nullable
    private IStorageCache<ItemStack> endinv$cache;

    @Unique
    private PortableGrid endinv$self() {
        return (PortableGrid) (Object) this;
    }

    @Unique
    @Nullable
    private IStorageDisk<ItemStack> endinv$fallbackStorage() {
        if (endinv$storage == null) {
            endinv$storage = RefinedStorageCompat.createStorage(endinv$self());
        }
        return endinv$storage;
    }

    @Unique
    @Nullable
    private IStorageCache<ItemStack> endinv$fallbackCache() {
        if (endinv$cache == null) {
            endinv$cache = RefinedStorageCompat.createCache(endinv$self());
        }
        return endinv$cache;
    }

    @Inject(method = "getStorage", at = @At("RETURN"), cancellable = true, remap = false)
    private void endinv$endlessStorage(CallbackInfoReturnable<IStorageDisk> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        IStorageDisk<ItemStack> fallback = endinv$fallbackStorage();
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = "getCache", at = @At("RETURN"), cancellable = true, remap = false)
    private void endinv$endlessCache(CallbackInfoReturnable<IStorageCache> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        IStorageCache<ItemStack> fallback = endinv$fallbackCache();
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = "getStorageCache", at = @At("RETURN"), cancellable = true, remap = false)
    private void endinv$endlessStorageCache(CallbackInfoReturnable<IStorageCache> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        IStorageCache<ItemStack> fallback = endinv$fallbackCache();
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = "isGridActive", at = @At("RETURN"), cancellable = true, remap = false)
    private void endinv$endlessGridActive(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        PortableGrid self = endinv$self();
        if (!ModInfo.getServerConfig().refinedStorageIgnoreEnergy().get() && RefinedStorageCompat.isEnergyBlocked(self)) {
            return;
        }
        if (RefinedStorageCompat.isFallbackAllowed(self)) {
            cir.setReturnValue(true);
        }
    }
}
