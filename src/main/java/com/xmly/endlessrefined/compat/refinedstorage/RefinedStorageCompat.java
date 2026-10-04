package com.xmly.endlessrefined.compat.refinedstorage;

import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.cache.InvalidateCause;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.apiimpl.storage.cache.PortableItemStorageCache;
import com.refinedmods.refinedstorage.blockentity.grid.portable.PortableGrid;
import com.refinedmods.refinedstorage.item.blockitem.PortableGridBlockItem;
import com.xmly.endlessrefined.config.ERIConfig;
import com.xmly.endlessrefined.endless.EndlessBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public final class RefinedStorageCompat {
    private RefinedStorageCompat() {
    }

    public static boolean hasDisk(PortableGrid grid) {
        return !grid.getDiskInventory().getStackInSlot(0).isEmpty();
    }

    public static boolean isFallbackAllowed(PortableGrid grid) {
        return ERIConfig.ENABLE_RS_INTEGRATION.get() && !hasDisk(grid);
    }

    public static boolean isFallbackUsable(PortableGrid grid) {
        if (!isFallbackAllowed(grid)) {
            return false;
        }
        Player player = grid.getPlayer();
        if (player == null || player.level().isClientSide) {
            return false;
        }
        return EndlessBridge.forPlayer(player) != null;
    }

    public static boolean isEnergyBlocked(PortableGrid grid) {
        if (!RS.SERVER_CONFIG.getPortableGrid().getUseEnergy()) {
            return false;
        }
        ItemStack stack = grid.getStack();
        if (!(stack.getItem() instanceof PortableGridBlockItem portableGrid)) {
            return true;
        }
        if (portableGrid.getType() == PortableGridBlockItem.Type.CREATIVE) {
            return false;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY, null).map(IEnergyStorage::getEnergyStored).map(stored -> stored <= RS.SERVER_CONFIG.getPortableGrid().getOpenUsage()).orElse(true);
    }

    @Nullable
    public static IStorageDisk<ItemStack> createStorage(PortableGrid grid) {
        return isFallbackUsable(grid) ? new EndlessPortableGridStorage(grid) : null;
    }

    @Nullable
    public static IStorageCache<ItemStack> createCache(PortableGrid grid) {
        if (!isFallbackAllowed(grid)) {
            return null;
        }
        IStorageCache<ItemStack> cache = new PortableItemStorageCache(grid);
        cache.invalidate(InvalidateCause.DISK_INVENTORY_CHANGED);
        return cache;
    }
}
