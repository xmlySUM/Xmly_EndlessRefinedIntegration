package com.kwwsyk.endinv.forge.integrates.refinedstorage;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.ServerLevelEndInv;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import com.mojang.logging.LogUtils;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.security.ISecurityCard;
import com.refinedmods.refinedstorage.api.storage.AccessType;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.cache.InvalidateCause;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorage;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.network.node.SecurityManagerNetworkNode;
import com.refinedmods.refinedstorage.apiimpl.storage.cache.ItemStorageCache;
import com.refinedmods.refinedstorage.blockentity.SecurityManagerBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**The Endless Inventory behind an External Storage bus attached to a Security Manager.<br>
 * Which player it is comes from the manager's own cards: the first one that holds every permission.
 * The inventory is looked up afresh on each tick, so putting a card in, taking one out or changing
 * what it grants takes effect straight away. It keeps working while that player is away, since an
 * Endless Inventory lives in the level save rather than on the player.
 *
 * <p>Refined Storage leaves an external storage to report its own changes: nothing else can see
 * into it, so {@link #update(INetwork)} is where the network is told what moved. It is told the
 * difference rather than the whole contents, which is both what the network expects and far less
 * work than rebuilding its view every time a single item is taken.
 */
public final class SecurityManagerExternalStorage implements IExternalStorage<ItemStack> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final IExternalStorageContext context;
    private final SecurityManagerBlockEntity securityManager;

    /**What the network was last told this storage holds. */
    private final Map<ItemKey, Integer> reported = new HashMap<>();

    /**The inventory those contents came from, so that a change of player is noticed. */
    @Nullable
    private EndlessInventory reportedOf;

    private long lastSeen = Long.MIN_VALUE;

    @Nullable
    private UUID reportedOwner;

    public SecurityManagerExternalStorage(IExternalStorageContext context, SecurityManagerBlockEntity securityManager) {
        this.context = context;
        this.securityManager = securityManager;
    }

    @Nullable
    private EndlessInventory endInv() {
        if (!ModInfo.getServerConfig().enableRefinedStorageNetwork().get()) {
            return null;
        }
        if (ServerLevelEndInv.levelEndInvData == null) {
            return null;
        }
        SecurityManagerNetworkNode node = securityManager.getNode();
        if (node == null) {
            return null;
        }
        List<ISecurityCard> cards = node.getCards();
        UUID owner = RefinedStorageCompat.firstFullPermissionOwner(cards);
        return owner == null ? null : RefinedStorageCompat.endInvOf(owner, securityManager.getLevel());
    }

    /**The owner this bus would use right now, for the log. */
    @Nullable
    private UUID currentOwner() {
        SecurityManagerNetworkNode node = securityManager.getNode();
        return node == null ? null : RefinedStorageCompat.firstFullPermissionOwner(node.getCards());
    }

    private static Map<ItemKey, Integer> countsOf(EndlessInventory endInv) {
        Map<ItemKey, Integer> counts = new HashMap<>();
        for (Map.Entry<ItemKey, ItemState> entry : endInv.snapshotItemMap().entrySet()) {
            counts.put(entry.getKey(), entry.getValue().count());
        }
        return counts;
    }

    /**Called every tick, as Refined Storage's contract for an outside storage.<br>
     * Reports what changed since the last look. That covers changes made through the network and
     * changes made by the player, /give, a command or another mod, because the inventory counts its
     * own modifications and this only has to compare two numbers to know whether to look.
     */
    @Override
    public void update(INetwork network) {
        EndlessInventory endInv = endInv();
        IStorageCache<ItemStack> cache = network.getItemStorageCache();

        if (endInv != reportedOf) {
            //A different player, or none at all. Nothing said about the old one still applies, so
            //the network's view is rebuilt from scratch rather than corrected.
            logOwnerChange(endInv);
            reportedOf = endInv;
            reported.clear();
            lastSeen = endInv == null ? Long.MIN_VALUE : endInv.getLastModTime();
            if (endInv != null) {
                reported.putAll(countsOf(endInv));
            }
            //Through the network's own queue: this runs inside its tick, and rebuilding the storage
            //from there is the sort of thing Refined Storage defers.
            network.getNodeGraph().runActionWhenPossible(
                    ItemStorageCache.INVALIDATE_ACTION.apply(InvalidateCause.DEVICE_CONFIGURATION_CHANGED));
            return;
        }

        if (endInv == null || cache == null) {
            return;
        }

        long signature = endInv.getLastModTime();
        if (signature == lastSeen) {
            return;
        }
        lastSeen = signature;

        Map<ItemKey, Integer> now = countsOf(endInv);
        for (Map.Entry<ItemKey, Integer> entry : now.entrySet()) {
            int was = reported.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > was) {
                cache.add(entry.getKey().toStack(entry.getValue() - was), entry.getValue() - was, false, false);
            }
        }
        for (Map.Entry<ItemKey, Integer> entry : reported.entrySet()) {
            int is = now.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > is) {
                cache.remove(entry.getKey().toStack(entry.getValue() - is), entry.getValue() - is, false);
            }
        }
        reported.clear();
        reported.putAll(now);
    }

    private void logOwnerChange(@Nullable EndlessInventory endInv) {
        UUID owner = currentOwner();
        if (owner != null ? owner.equals(reportedOwner) : reportedOwner == null) {
            //Same player; only the contents moved, which needs no announcement.
            if (owner == null) {
                return;
            }
        }
        reportedOwner = owner;
        if (owner == null) {
            LOGGER.info("A storage bus on a Security Manager has no player with every permission to draw on.");
        } else if (endInv == null) {
            LOGGER.info("A storage bus on a Security Manager has a player with every permission ({}) "
                    + "but no Endless Inventory to offer them.", owner);
        } else {
            LOGGER.info("A storage bus on a Security Manager is offering the Endless Inventory of {}.", owner);
        }
    }

    @Override
    public Collection<ItemStack> getStacks() {
        EndlessInventory endInv = endInv();

        return endInv == null ? List.of() : endInv.getItemsAsList();
    }

    @Override
    @Nonnull
    public ItemStack insert(@Nonnull ItemStack stack, int size, Action action) {
        if (stack.isEmpty() || size <= 0 || !context.acceptsItem(stack)) {
            return stack.copyWithCount(size);
        }

        EndlessInventory endInv = endInv();

        if (endInv == null) {
            return stack.copyWithCount(size);
        }

        ItemKey key = ItemKey.asKey(stack);

        if (action == Action.SIMULATE) {
            int accepted = endInv.acceptedCount(key, size);

            return accepted >= size ? ItemStack.EMPTY : stack.copyWithCount(size - accepted);
        }

        //The change is reported by update(), which is where Refined Storage expects an outside
        //storage to do its bookkeeping. Saying it here as well would count it twice.
        return endInv.addItem(key, size);
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
        int available = endInv.count(key);

        if (available <= 0) {
            return ItemStack.EMPTY;
        }

        // In infinity mode the pool never drains, so every request succeeds in full.
        boolean infinite = endInv.isInfinityMode() && available >= endInv.getMaxItemStackSize();
        int amount = infinite ? size : Math.min(size, available);

        if (action == Action.SIMULATE) {
            return key.toStack(amount);
        }

        ItemStack taken = endInv.takeItem(key, amount);

        if (taken.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // takeItem hands back the whole pool in infinity mode; the caller only asked for `amount`.
        if (taken.getCount() > amount) {
            taken.setCount(amount);
        }

        return taken;
    }

    @Override
    public int getStored() {
        EndlessInventory endInv = endInv();

        return endInv == null ? 0 : endInv.storedTotal();
    }

    @Override
    public int getPriority() {
        return context.getPriority();
    }

    @Override
    public AccessType getAccessType() {
        return context.getAccessType();
    }

    @Override
    public int getCacheDelta(int storedPreInsertion, int size, @Nullable ItemStack remainder) {
        return size - (remainder == null || remainder.isEmpty() ? 0 : remainder.getCount());
    }

    /**Only as large as there is something to put in it.<br>
     * Reporting a capacity for a manager that offers nothing made an empty bus look like a working
     * one that happened to be empty.
     */
    @Override
    public long getCapacity() {
        return endInv() == null ? 0 : Integer.MAX_VALUE;
    }
}
