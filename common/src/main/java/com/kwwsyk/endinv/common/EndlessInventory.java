package com.kwwsyk.endinv.common;


import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvContent;
import com.kwwsyk.endinv.common.network.payloads.toClient.EndInvContentDelta;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemStackLike;
import com.kwwsyk.endinv.common.util.ItemState;
import com.kwwsyk.endinv.common.util.SortType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.*;

/**An item container may have endless storage.*/
public class EndlessInventory extends SourceInventory {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("unchecked")
    private final List<ItemStack>[] sortedViews = new List[SortType.values().length];

    private final long[] lastSortedTimes = new long[SortType.values().length];

    public final EndInvAffinities affinities;

    public List<ServerPlayer> viewers = new ArrayList<>();

    /**The change counter the viewers were last told about, so that a tick with nothing to say
     * costs one comparison.
     */
    private long lastBroadcast = Long.MIN_VALUE;

    /**The contents the viewers were last told about, so that what changed since can be worked out.
     * Null until something has been told at all.
     */
    private Map<ItemKey, ItemState> broadcastBaseline;

    public EndlessInventory(){
        this(UUID.randomUUID());
    }

    public EndlessInventory(UUID uuid){
        super(uuid);
        this.affinities = new EndInvAffinities(this);
    }

    protected List<ItemStack> getSortedView(SortType type, boolean reverse) {
        int idx = type.ordinal();
        List<ItemStack> result;
        synchronized (sortedViews) {
            if (lastSortedTimes[idx] != lastModTime || sortedViews[idx] == null) {
                List<ItemStack> view = snapshotItems();
                view.sort(ModInfo.sortHelper.getComparator(type, this));
                sortedViews[idx] = view;
                lastSortedTimes[idx] = lastModTime;
            }
            result = new ArrayList<>(sortedViews[idx]);
        }
        if(reverse) Collections.reverse(result);
        return result;
    }

    public List<ItemStackLike> getStarredItems(@Nonnegative int startIndex, @Nonnegative int length){
        var items = affinities.getStarredItems(startIndex,length);
        return items.stream().map(this::getStackWithZeroCount).toList();
    }

    public List<ItemStackLike> getStarredItems(){
        return getStarredItems(0,affinities.starredItems.size());
    }

    public ItemStackLike getStackWithZeroCount(ItemStack stack){
        var state = itemMap.get(ItemKey.asKey(stack));
        if(state==null) return ItemStackLike.asKey(stack);
        return ItemStackLike.asKey(stack,state.count());
    }

    @Nullable
    public Optional<ServerPlayer> getOwner(ServerLevel level) {
        return level.getPlayers(pl->Objects.equals(pl.getUUID(),owner)).stream().findAny();
    }

    public void setChanged() {
        super.setChanged();
        ServerLevelEndInv.levelEndInvData.setDirty();
    }

    /**
     * Set endinv modState to new greater state.
     * @param newState should be greater than its original state
     * @return endinv's modState that has been updated
     */
    public long updateModState(long newState){
        if (newState <= lastModTime) {
            newState = lastModTime + 1;
        }
        this.lastModTime = newState;
        return lastModTime;
    }

    /**Tell everyone looking at this inventory that it changed.<br>
     * Without this the contents only move when a viewer asks, so anything that changes them from
     * outside a viewer's own screen - a Refined Storage network drawing on the inventory, another
     * player, a command - left their screen showing what used to be there.
     * Called every server tick and cheap unless something actually changed: the inventory already
     * counts its own modifications.
     */
    public void broadcastChanges(){
        if(viewers.isEmpty() || lastBroadcast == lastModTime) return;
        lastBroadcast = lastModTime;

        Map<ItemKey, ItemState> now = snapshotItemMap();
        if(broadcastBaseline == null){
            //Nothing has been sent yet: the screen's own opening sync carries the whole inventory,
            //so there is nothing here to correct it with.
            broadcastBaseline = now;
            return;
        }

        Map<ItemKey, ItemState> changed = new HashMap<>();
        for(Map.Entry<ItemKey, ItemState> entry : now.entrySet()){
            ItemState before = broadcastBaseline.get(entry.getKey());
            if(before == null || before.count() != entry.getValue().count()){
                changed.put(entry.getKey(), entry.getValue());
            }
        }
        List<ItemKey> removed = new ArrayList<>();
        for(ItemKey key : broadcastBaseline.keySet()){
            if(!now.containsKey(key)) removed.add(key);
        }
        broadcastBaseline = now;

        if(changed.isEmpty() && removed.isEmpty()) return;
        EndInvContentDelta delta = new EndInvContentDelta(changed, removed);
        for(ServerPlayer viewer : viewers){
            if(viewer.hasDisconnected()) continue;
            ModInfo.getPacketDistributor().sendToPlayer(viewer, delta);
        }
    }
}
