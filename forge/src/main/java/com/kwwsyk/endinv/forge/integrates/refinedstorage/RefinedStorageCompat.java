package com.kwwsyk.endinv.forge.integrates.refinedstorage;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.forge.EndInvAccess;
import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.api.network.security.ISecurityCard;
import com.refinedmods.refinedstorage.api.network.security.ISecurityManager;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.cache.InvalidateCause;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.apiimpl.storage.cache.PortableItemStorageCache;
import com.refinedmods.refinedstorage.blockentity.grid.portable.PortableGrid;
import com.refinedmods.refinedstorage.item.blockitem.PortableGridBlockItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**A portable grid with no storage disk normally refuses to work. With this, it opens anyway and
 * is backed by the player's Endless Inventory; with a disk inserted nothing changes.
 */
public final class RefinedStorageCompat {

    private RefinedStorageCompat() {
    }

    public static boolean hasDisk(PortableGrid grid) {
        return !grid.getDiskInventory().getStackInSlot(0).isEmpty();
    }

    public static boolean isFallbackAllowed(PortableGrid grid) {
        return ModInfo.getServerConfig().enableRefinedStorage().get() && !hasDisk(grid);
    }

    public static boolean isFallbackUsable(PortableGrid grid) {
        if (!isFallbackAllowed(grid)) {
            return false;
        }
        Player player = grid.getPlayer();
        if (player == null || player.level().isClientSide) {
            return false;
        }
        return EndInvAccess.of(player) != null;
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

    /**The first player on a security manager who holds every permission, if there is one.<br>
     * The cards come back in the order of the security manager's slots, so "first" means the first
     * slot and stays the same between calls. Reading them off the manager itself rather than
     * through {@link ISecurityManager}, which only answers about one player at a time.
     * @return the chosen player's id, or null when no card grants everything
     */
    @Nullable
    public static UUID firstFullPermissionOwner(List<ISecurityCard> cards) {
        for (ISecurityCard card : cards) {
            UUID owner = card.getOwner();
            if (owner != null && grantsEverything(card)) {
                return owner;
            }
        }
        return null;
    }

    /**The Endless Inventory belonging to a player, whether or not they are online.<br>
     * Goes through the record the mod keeps on the player when they are here, and falls back to the
     * inventory's own owner when they are not - the id on the player is only in the player's data,
     * so it is no use once they have gone.
     */
    @Nullable
    public static EndlessInventory endInvOf(@Nullable UUID playerId, Level level) {
        if (playerId == null || ServerLevelEndInv.levelEndInvData == null) {
            return null;
        }
        if (level instanceof ServerLevel serverLevel) {
            Player online = serverLevel.getPlayerByUUID(playerId);
            if (online != null) {
                EndlessInventory assigned = ServerLevelEndInv.getAssignedEndInv(online);
                if (assigned != null) {
                    return assigned;
                }
            }
        }
        return ServerLevelEndInv.levelEndInvData.fromOwner(playerId);
    }

    private static boolean grantsEverything(ISecurityCard card) {
        for (Permission permission : Permission.values()) {
            if (!card.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

}
