package com.xmly.endlessrefined.hotbar;

import com.kwwsyk.endinv.common.util.ItemKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HotbarTable {

    public static final int ROWS = 9;
    public static final int COLUMNS = 9;
    public static final int CELLS = ROWS * COLUMNS;

    private static final String TABLE_TAG = "EndlessRefinedHotbarTable";
    private static final String SUPPRESSED_TAG = "EndlessRefinedHotbarSuppressed";
    private static final String CELL_KEY = "Key";

    private final ItemKey[] keys = new ItemKey[CELLS];

    private final Set<ItemKey> suppressed = new HashSet<>();

    @Nullable
    public ItemKey keyAt(int index) {
        return isValid(index) ? keys[index] : null;
    }

    public boolean isEmpty() {
        return claimedCells() == 0;
    }

    public int claimedCells() {
        int claimed = 0;

        for (ItemKey key : keys) {
            if (key != null) {
                claimed++;
            }
        }

        return claimed;
    }

    public List<ItemStack> cells() {
        List<ItemStack> cells = new java.util.ArrayList<>(CELLS);

        for (ItemKey key : keys) {
            cells.add(key == null ? ItemStack.EMPTY : key.toStack(1));
        }

        return cells;
    }

    public int indexOf(ItemKey key) {
        for (int i = 0; i < CELLS; i++) {
            if (key.equals(keys[i])) {
                return i;
            }
        }

        return -1;
    }

    public int firstEmptyCell() {
        for (int i = 0; i < CELLS; i++) {
            if (keys[i] == null) {
                return i;
            }
        }

        return -1;
    }

    public int claim(ItemKey key) {
        int existing = indexOf(key);

        if (existing >= 0) {
            return existing;
        }

        int empty = firstEmptyCell();

        if (empty < 0) {
            return -1;
        }

        keys[empty] = key;

        return empty;
    }

    public boolean clear(int index) {
        if (!isValid(index) || keys[index] == null) {
            return false;
        }

        keys[index] = null;

        return true;
    }

    public boolean remove(int index) {
        if (!isValid(index) || keys[index] == null) {
            return false;
        }

        suppressed.add(keys[index]);
        keys[index] = null;

        return true;
    }

    public void set(int index, ItemKey key) {
        if (!isValid(index)) {
            return;
        }

        int existing = indexOf(key);

        if (existing >= 0 && existing != index) {
            keys[existing] = null;
        }

        suppressed.remove(key);
        keys[index] = key;
    }


    public void suppress(ItemKey key) {
        suppressed.add(key);
    }

    public int place(ItemKey key) {
        suppressed.remove(key);

        return claim(key);
    }


    public void sync(List<ItemStack> stockItems) {
        for (ItemStack item : stockItems) {
            if (item.isEmpty()) {
                continue;
            }

            // Keys are taken straight from what Endless reported, so they compare equal to
            // the ones it will hand back when the item is drawn.
            ItemKey key = ItemKey.asKey(item);

            if (indexOf(key) >= 0 || suppressed.contains(key)) {
                continue;
            }

            if (claim(key) < 0) {
                return;
            }
        }
    }

    public void load(CompoundTag data) {
        ListTag list = data.getList(TABLE_TAG, Tag.TAG_COMPOUND);

        for (int i = 0; i < CELLS && i < list.size(); i++) {
            CompoundTag cell = list.getCompound(i);

            if (!cell.contains(CELL_KEY)) {
                continue;
            }

            ItemStack keyStack = ItemStack.of(cell.getCompound(CELL_KEY));

            if (!keyStack.isEmpty()) {
                keys[i] = ItemKey.asKey(keyStack);
            }
        }

        ListTag offHotbar = data.getList(SUPPRESSED_TAG, Tag.TAG_COMPOUND);

        for (int i = 0; i < offHotbar.size(); i++) {
            ItemStack keyStack = ItemStack.of(offHotbar.getCompound(i));

            if (!keyStack.isEmpty()) {
                suppressed.add(ItemKey.asKey(keyStack));
            }
        }
    }

    public void save(CompoundTag data) {
        ListTag list = new ListTag();

        for (int i = 0; i < CELLS; i++) {
            CompoundTag cell = new CompoundTag();

            if (keys[i] != null) {
                cell.put(CELL_KEY, keys[i].toStack(1).save(new CompoundTag()));
            }

            list.add(cell);
        }

        data.put(TABLE_TAG, list);

        ListTag offHotbar = new ListTag();

        for (ItemKey key : suppressed) {
            offHotbar.add(key.toStack(1).save(new CompoundTag()));
        }

        data.put(SUPPRESSED_TAG, offHotbar);
    }

    public static void copy(CompoundTag from, CompoundTag to) {
        Tag saved = from.get(TABLE_TAG);

        if (saved != null) {
            to.put(TABLE_TAG, saved.copy());
        }
    }

    private static boolean isValid(int index) {
        return index >= 0 && index < CELLS;
    }
}
