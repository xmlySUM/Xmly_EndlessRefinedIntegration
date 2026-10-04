package com.xmly.endlessrefined.mixin.refinedstorage;

import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.blockentity.grid.portable.PortableGrid;
import com.xmly.endlessrefined.compat.refinedstorage.RefinedStorageCompat;
import com.xmly.endlessrefined.config.ERIConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(PortableGrid.class)
@SuppressWarnings({"rawtypes"})
public abstract class PortableGridMixin {
    @Unique
    @Nullable
    private IStorageDisk<ItemStack> eri$storage;
    @Unique
    @Nullable
    private IStorageCache<ItemStack> eri$cache;

    @Unique
    private PortableGrid eri$self() {
        return (PortableGrid) (Object) this;
    }

    @Unique
    @Nullable
    private IStorageDisk<ItemStack> eri$fallbackStorage() {
        if (eri$storage == null) {
            eri$storage = RefinedStorageCompat.createStorage(eri$self());
        }
        return eri$storage;
    }

    @Unique
    @Nullable
    private IStorageCache<ItemStack> eri$fallbackCache() {
        if (eri$cache == null) {
            eri$cache = RefinedStorageCompat.createCache(eri$self());
        }
        return eri$cache;
    }

    @Inject(method = "getStorage", at = @At("RETURN"), cancellable = true, remap = false)
    private void eri$endlessStorage(CallbackInfoReturnable<IStorageDisk> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        IStorageDisk<ItemStack> fallback = eri$fallbackStorage();
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = "getCache", at = @At("RETURN"), cancellable = true, remap = false)
    private void eri$endlessCache(CallbackInfoReturnable<IStorageCache> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        IStorageCache<ItemStack> fallback = eri$fallbackCache();
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = "getStorageCache", at = @At("RETURN"), cancellable = true, remap = false)
    private void eri$endlessStorageCache(CallbackInfoReturnable<IStorageCache> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        IStorageCache<ItemStack> fallback = eri$fallbackCache();
        if (fallback != null) {
            cir.setReturnValue(fallback);
        }
    }

    @Inject(method = "isGridActive", at = @At("RETURN"), cancellable = true, remap = false)
    private void eri$endlessGridActive(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        PortableGrid self = eri$self();
        if (!ERIConfig.RS_IGNORE_ENERGY.get() && RefinedStorageCompat.isEnergyBlocked(self)) {
            return;
        }
        if (RefinedStorageCompat.isFallbackAllowed(self)) {
            cir.setReturnValue(true);
        }
    }
}
