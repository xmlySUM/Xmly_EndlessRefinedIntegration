package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.util.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kwwsyk.endinv.common.ModInfo.getServerConfig;

/**Holder of core and shared ({@link EndlessInventory}&{@link com.kwwsyk.endinv.common.client.CachedSrcInv} endless INVENTORY logic.
 */
public abstract class SourceInventory {

    private static final Logger LOGGER = LogUtils.getLogger();

    //item container
    protected final Map<ItemKey, ItemState> itemMap;
    protected List<ItemStack> items;

    //meta
    protected UUID uuid;
    protected volatile long lastModTime = Util.getMillis();
    protected int maxStackSize;
    protected boolean infinityMode;
    //accessibility attributes
    @Nullable
    protected UUID owner;
    public List<UUID> white_list = new ArrayList<>();
    protected Accessibility accessibility;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private volatile boolean itemsDirty = true;

    public SourceInventory(UUID uuid){
        this.items = new ArrayList<>();
        this.itemMap = new Object2ObjectLinkedOpenHashMap<>();
        this.uuid = uuid;
        this.maxStackSize = ModInfo.getServerConfig().getMaxAllowedStackSize().get();
        this.infinityMode = ModInfo.getServerConfig().allowInfinityMode().get();
        this.accessibility = ModInfo.getServerConfig().defaultAccessibility().get();
    }


    public void setChanged(){
        markCacheDirty();
    }

    //field accessor
    public UUID getUuid(){
        return this.uuid;
    }

    public UUID giveNewUuid(){
        uuid=UUID.randomUUID();
        return uuid;
    }

    public Map<ItemKey,ItemState> snapshotItemMap(){
        return new Object2ObjectLinkedOpenHashMap<>(itemMap);
    }

    public Map<ItemKey,ItemState> getItemMap(){
        return itemMap;
    }

    /**When this inventory last changed, as a number that only ever grows.<br>
     * Cheap enough to compare every tick, which is how something that has to notice outside
     * changes - an external storage on a Refined Storage network, say - can tell whether it needs
     * to look again without diffing the contents.
     */
    public long getLastModTime(){
        return lastModTime;
    }

    /**Merge a partial snapshot into the store, replacing entries that are already known.<br>
     * Used by the client cache when the server sends the contents of one page in
     * {@code TransferMode.PART}. {@link #snapshotItemMap()} hands out a copy, so merging into the
     * result of that call would write into a throwaway map and leave the cache untouched.
     */
    public void applyPartialSnapshot(Map<ItemKey,ItemState> snapshot){
        writeLock.lock();
        try {
            this.itemMap.putAll(snapshot);
            updateLastModTime();
            setChanged();
        } finally {
            writeLock.unlock();
        }
    }

    /**Apply a set of changes from the server.<br>
     * The counterpart of {@link #applyPartialSnapshot}, for the small updates that follow a screen
     * being opened: only what moved travels, everything else stays as it was.
     */
    public void applyDelta(Map<ItemKey,ItemState> changed, List<ItemKey> removed){
        writeLock.lock();
        try {
            this.itemMap.putAll(changed);
            for(ItemKey key : removed){
                this.itemMap.remove(key);
            }
            updateLastModTime();
            setChanged();
        } finally {
            writeLock.unlock();
        }
    }

    /**How many of the given item the store holds, counting the other spelling of "no tag".<br>
     * Unlike {@link #snapshotItemMap()} this reads the entry itself, so it is the cheap way for
     * callers that only want a count.
     */
    public int count(ItemKey key){
        readLock.lock();
        try {
            Map.Entry<ItemKey,ItemState> match = matchEntry(key);
            return match==null ? 0 : match.getValue().count();
        } finally {
            readLock.unlock();
        }
    }

    /**How many more of the given item the store would take in one go.<br>
     * Answers the same question {@link #addItem(ItemKey, int)} answers, without changing anything,
     * which is what a caller that has to simulate an insertion needs.
     */
    public int acceptedCount(ItemKey key, int size){
        if(size<=0) return 0;
        int original = count(key);
        if(original >= maxStackSize) return infinityMode ? size : 0;
        long increased = (long) original + size;
        long accepted = Math.min(increased, maxStackSize) - original;
        return (int) Math.max(0L, Math.min((long) size, accepted));
    }

    /**Every item in the store added up, saturating at {@link Integer#MAX_VALUE}. */
    public int storedTotal(){
        readLock.lock();
        try {
            long total = 0L;
            for (ItemState state : itemMap.values()) {
                total += state.count();
                if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            }
            return (int) total;
        } finally {
            readLock.unlock();
        }
    }

    /**Look up an item, falling back to the other spelling of an absent tag.<br>
     * An item with no tag and the same item with an empty {@link CompoundTag} are not equal as
     * keys, so a store can hold the same thing twice under the two spellings. This is the one place
     * that rule lives; the returned entry's key is the spelling that actually matched and must be
     * used for any later write. Gated by {@code doConvertEmptyTag}, off by default.
     * @return the entry found, or null
     */
    @Nullable
    private Map.Entry<ItemKey,ItemState> matchEntry(ItemKey key){
        ItemState state = itemMap.get(key);
        if(state!=null) return Map.entry(key,state);
        if(!getServerConfig().doConvertEmptyTag().get()) return null;
        ItemKey alternate = alternateTagKey(key);
        state = itemMap.get(alternate);
        return state==null ? null : Map.entry(alternate,state);
    }

    /**The same key with an absent tag written as an empty one, or the other way round. */
    private static ItemKey alternateTagKey(ItemKey key){
        if(key.tag()==null) return new ItemKey(key.item(), new CompoundTag());
        if(key.tag().isEmpty()) return new ItemKey(key.item(), null);
        return key;
    }

    public List<ItemStack> getItemsAsList(){
        return snapshotItems();
    }

    /**
     * @return Size of Endless Inventory.
     */
    public int getItemSize() {
        readLock.lock();
        try {
            if (!itemsDirty) {
                return this.items.size();
            }
        } finally {
            readLock.unlock();
        }
        writeLock.lock();
        try {
            if (itemsDirty) {
                refreshItemsFromMap();
            }
            return this.items.size();
        } finally {
            writeLock.unlock();
        }
    }

    public int getMaxItemStackSize() {
        return maxStackSize;
    }

    public void setMaxItemStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    public boolean isInfinityMode() {
        return infinityMode;
    }

    public void setInfinityMode(boolean infinityMode) {
        this.infinityMode = infinityMode;
    }

    public Accessibility getAccessibility() {
        return accessibility;
    }

    public void setAccessibility(Accessibility accessibility) {
        this.accessibility = accessibility;
    }

    public boolean accessible(Player player) {
        return accessibility==Accessibility.PUBLIC || (owner!=null && owner.equals(player.getUUID())) || white_list.contains(player.getUUID()) && accessibility==Accessibility.RESTRICTED;
    }

    public boolean isOwner(Player player){
        return Objects.equals(player.getUUID(),owner);
    }

    @Nullable
    public UUID getOwnerUUID(){
        return owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }


    //item handler methods: item modification
    /**
     * Take at most its max stack size count item
     * @param itemStack will take itemStack with same id and component
     * @return items taken
     */
    public ItemStack takeItem(ItemStack itemStack) {
        return takeItem(itemStack,itemStack.getMaxStackSize());
    }

    /**
     * Take away those itemStack having same id&component with given itemStack in EndInv
     *
     * @param stack given item, with id and component | 物品堆叠，id和组件的提供器
     * @param count the max count of items taken away | 拿走的最大数目
     * @return taken items | 被拿走的物品
     */
    public ItemStack takeItem(ItemStack stack, int count){
        if(stack.isEmpty()) return ItemStack.EMPTY;
        return takeItem(ItemKey.asKey(stack), count);
    }

    public ItemStack takeItem(ItemKey key, int count){
        writeLock.lock();
        try {
            Map.Entry<ItemKey,ItemState> match = matchEntry(key);
            if (match == null) {
                LOGGER.warn("EI:takeItem: no state for {}", key);
                LOGGER.debug("EI:Current Source Inventory: Class:{},UUID:{},Items:{}",this.getClass(),uuid,buildSnapshotLocked());
                return ItemStack.EMPTY;
            }
            //The entry may be filed under the other spelling of "no tag"; carry on with that key.
            key = match.getKey();
            ItemState state = match.getValue();
            LOGGER.debug("EI:takeItem: key={} availableCount={} requestedCount={}", key, state.count(), count);
            if(state.count() >= maxStackSize && infinityMode){
                LOGGER.info("EI:takeItem: infinity mode for {} returning {}", key, count);
                setChanged();
                return key.toStack(state.count());
            }

            int taken = Math.min(count, state.count());
            ItemStack result = key.toStack(taken);
            if (taken == state.count()) {
                itemMap.remove(key);
                updateLastModTime();
                LOGGER.debug("EI:takeItem: removed all of {} (taken={})", key, taken);
            } else {
                itemMap.put(key, new ItemState(state.count() - taken, updateLastModTime()));
                LOGGER.debug("EI:takeItem: taken={} remaining={} for {}", taken, state.count() - taken, key);
            }
            setChanged();
            return result;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Add item to EndInv and return remain item copy.
     * If adding is performed, update EndInv State by invoking {@link #setChanged()},
     * which is when some item inserted, including infinite mode inserting.
     * @param itemStack to add
     * @return Remain item copied, or {@link ItemStack#EMPTY} if all inserted.
     */
    public ItemStack addItem(ItemStack itemStack){
        if(itemStack.isEmpty()) return ItemStack.EMPTY;
        return addItem(ItemKey.asKey(itemStack), itemStack.getCount());
    }

    public ItemStack addItem(ItemKey key, int count){
        writeLock.lock();
        try {
            //Adding has to agree with takeItem about which spelling of "no tag" is the entry,
            //otherwise one item ends up stored twice.
            Map.Entry<ItemKey,ItemState> match = matchEntry(key);
            if (match != null) {
                key = match.getKey();
            }
            ItemState state = match == null ? null : match.getValue();
            int original = 0;

            if (state != null) {
                original = state.count();
            }
            int increased;
            ItemStack remain;
            if(original < maxStackSize){
                increased = original+count;
                if(increased <= maxStackSize){
                    itemMap.put(key, new ItemState(increased, updateLastModTime()));
                    remain = ItemStack.EMPTY;
                }else {
                    itemMap.put(key, new ItemState(maxStackSize, updateLastModTime()));
                    remain = key.toStack(increased-maxStackSize);
                }
            }else if(infinityMode){
                itemMap.put(key, new ItemState(original, updateLastModTime()));
                remain = ItemStack.EMPTY;
            }else {
                return key.toStack(count);
            }
            setChanged();
            return remain;
        } finally {
            writeLock.unlock();
        }
    }

    public void clearContent() {
        writeLock.lock();
        try {
            this.itemMap.clear();
            updateLastModTime();
            this.setChanged();
        } finally {
            writeLock.unlock();
        }
    }

    //item handler: item view methods

    protected List<ItemStack> getSortedView(SortType type, boolean reverse) {
        List<ItemStack> ret = snapshotItems();
        ret.sort(ModInfo.sortHelper.getComparator(type, this));
        if(reverse) Collections.reverse(ret);
        return ret;
    }

    public List<ItemStack> getSortedAndFilteredItemView(int startIndex,
                                                        int length,
                                                        SortType sortType,
                                                        boolean reverse,
                                                        @Nullable Predicate<ItemStack> classify,
                                                        String search
    ){
        Stream<ItemStack> base = getSortedView(sortType,reverse).stream();
        return base
                .filter(classify!=null?classify:is->true)
                .filter(stack -> SearchUtil.matchesSearch(stack,search))
                .skip(startIndex)
                .limit(Math.max(length, 0))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Stream<ItemKey> getSortedKeyReference(SortType sortType){
        return switch (sortType){
            case ID -> this.snapshotItemMap().keySet()
                    .stream()
                    .sorted(Comparator.comparingInt(key -> BuiltInRegistries.ITEM.getId(key.item())));
            case DEFAULT -> this.snapshotItemMap().keySet()
                    .stream();
            case COUNT -> this.snapshotItemMap()
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(e -> e.getValue().count()))
                    .map(Map.Entry::getKey);
            case SPACE_AND_NAME -> this.snapshotItemMap()
                    .keySet()
                    .stream()
                    .sorted(Comparator.comparing(key -> BuiltInRegistries.ITEM.getKey(key.item()).toString()));
            case LAST_MODIFIED -> this.snapshotItemMap()
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(e -> e.getValue().lastModTime()))
                    .map(Map.Entry::getKey);
        };
    }

    //item handler: map-list sync methods

    public void syncItemsFromMap() {
        writeLock.lock();
        try {
            refreshItemsFromMap();
        } finally {
            writeLock.unlock();
        }
    }

    public void syncMapFromItems() {
        writeLock.lock();
        try {
            this.itemMap.clear();
            for (ItemStack stack : items) {
                if (stack.isEmpty()) continue;
                long now = updateLastModTime();
                var key = ItemKey.asKey(stack);
                this.itemMap.put(key, new ItemState(stack.getCount(), now));
            }
            markCacheDirty();
        } finally {
            writeLock.unlock();
        }
    }

    //modification version mangers
    public long updateLastModTime(){
        long now = Util.getMillis();
        if (now <= lastModTime) {
            now = lastModTime + 1;
        }
        lastModTime = now;
        return lastModTime;
    }

    protected List<ItemStack> snapshotItems() {
        readLock.lock();
        try {
            if (!itemsDirty) {
                return new ArrayList<>(items);
            }
        } finally {
            readLock.unlock();
        }
        writeLock.lock();
        try {
            if (itemsDirty) {
                refreshItemsFromMap();
            }
            return new ArrayList<>(items);
        } finally {
            writeLock.unlock();
        }
    }

    private void refreshItemsFromMap() {
        this.items = itemMap.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue().count()))
                .collect(Collectors.toList());
        itemsDirty = false;
    }

    private List<ItemStack> buildSnapshotLocked() {
        return itemMap.entrySet().stream()
                .map(e -> e.getKey().toStack(e.getValue().count()))
                .collect(Collectors.toList());
    }

    private void markCacheDirty() {
        itemsDirty = true;
    }

    protected void overwriteItems(Map<ItemKey, ItemState> newItems) {
        writeLock.lock();
        try {
            this.itemMap.clear();
            this.itemMap.putAll(newItems);
            markCacheDirty();
            updateLastModTime();
        } finally {
            writeLock.unlock();
        }
    }
}
