package com.xmly.endlessrefined.hotbar;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.kwwsyk.endinv.common.util.ItemKey;
import com.kwwsyk.endinv.common.util.ItemState;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import com.xmly.endlessrefined.config.ERIConfig;
import com.xmly.endlessrefined.endless.EndlessBridge;
import com.xmly.endlessrefined.network.HotbarNoticeS2C;
import com.xmly.endlessrefined.network.HotbarTableS2C;
import com.xmly.endlessrefined.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HotbarServerState {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    public static final int EXTENDED_FIRST = Inventory.getSelectionSize();

    public static final int EXTENDED_LAST = Inventory.INVENTORY_SIZE;

    private static final int EXTENDED_COUNT = EXTENDED_LAST - EXTENDED_FIRST;

    private static final int RESYNC_INTERVAL_TICKS = 10;

    private static final String ENDER_CHEST_TAG = "EndlessRefinedEnderChestMode";

    private static final Map<UUID, HotbarServerState> STATES = new HashMap<>();

    private final HotbarTable table = new HotbarTable();

    private final ItemKey[] drawn = new ItemKey[EXTENDED_COUNT];

    private final int[] drawnStock = new int[EXTENDED_COUNT];

    private int digit = 0;
    private int clientGroups = 1;
    private boolean holding = false;

    private int heldCovered = 0;

    private int resyncTimer = 0;
    private int persistedCells = -1;

    private boolean panelExpanded = false;

    private boolean screenOpen = false;

    private boolean enderChest = false;

    public void setScreenOpen(boolean open) {
        this.screenOpen = open;
    }

    public boolean isScreenOpen() {
        return screenOpen;
    }

    private HotbarServerState() {
    }

    private static int slotFor(int rowIndex, int column) {
        return (rowIndex + 1) * 9 + column;
    }

    // ------------------------------------------------------------------ static

    public static HotbarServerState get(ServerPlayer player) {
        return STATES.get(player.getUUID());
    }

    public static HotbarServerState of(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> {
            HotbarServerState state = new HotbarServerState();
            state.table.load(player.getPersistentData());
            state.persistedCells = state.table.claimedCells();

            return state;
        });
    }

    public static void recover(ServerPlayer player) {
        STATES.remove(player.getUUID());

        CompoundTag data = player.getPersistentData();

        if (!data.getBoolean(ENDER_CHEST_TAG)) {
            return;
        }

        stowIntoEnderChest(player);
        data.remove(ENDER_CHEST_TAG);
    }

    public static void forget(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static void stow(ServerPlayer player) {
        HotbarServerState state = STATES.get(player.getUUID());

        if (state != null && state.enderChest) {
            state.leaveEnderChest(player);
        }
    }

    public static boolean ownsSlot(ServerPlayer player, int inventorySlot) {
        HotbarServerState state = STATES.get(player.getUUID());

        return state != null && state.ownsSlotInternal(inventorySlot);
    }

    public static void tick(ServerPlayer player) {
        HotbarServerState state = STATES.get(player.getUUID());

        if (state != null) {
            state.tickInternal(player);
        }
    }

    public void apply(ServerPlayer player, int newDigit, int groups, boolean pageKey) {
        int previousGroups = clientGroups;
        int previousDigit = digit;
        boolean wasHolding = holding;

        clientGroups = HotbarGroups.clampConfigured(groups);

        if (newDigit == 0) {
            // Digit 0 arrives for two unrelated reasons: a Ctrl+0 press, and the client simply
            // reporting how many groups it is set to. Only the press does anything.
            if (pageKey) {
                // ========== 修复逻辑开始 ==========
                if (holding) {
                    // 当前正在使用扩展热栏映射：Ctrl+0 关闭映射，不进入末影箱
                    stop(player);
                    NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.hotbar_closed"));
                } else {
                    // 没有打开热栏映射：才允许切换末影箱，并且必须groups>=2
                    if (clientGroups >= 2) {
                        toggleEnderChest(player);
                    } else {
                        // groups<2，禁止末影箱，提示
                        NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.no_groups"));
                    }
                }
                // ========== 修复逻辑结束 ==========
            }
            return;
        }

        int[] rows = HotbarGroups.visibleRows(clientGroups, newDigit);

        if (rows.length == 0) {
            return;
        }

        if (enderChest) {
            leaveEnderChest(player);
        }

        if (wasHolding && !Arrays.equals(HotbarGroups.visibleRows(previousGroups, previousDigit), rows)) {
            vacateDrawn(player);
        }

        if (!holding) {
            holding = true;
            heldCovered = 0;
        }

        boolean moved = resizeCoverage(player, coveredSlots());

        digit = newDigit;
        resyncTimer = 0;
        resync(player);
        maintainSlots(player);

        if (moved) {
            // Worth saying: the items are in Endless rather than gone, and the panel is how
            // they go back on.
            NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.displaced"));
        }

        if (table.isEmpty()) {
            NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.empty_table"));
        }
    }

    private void vacateDrawn(ServerPlayer player) {
        EndlessInventory endInv = EndlessBridge.forPlayer(player);

        if (endInv == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        boolean changed = false;

        for (int i = 0; i < heldCovered; i++) {
            ItemKey drawnKey = drawn[i];

            if (drawnKey == null) {
                continue;
            }

            drawn[i] = null;

            int slot = EXTENDED_FIRST + i;
            ItemStack current = inventory.getItem(slot);

            if (current.isEmpty() || !drawnKey.equals(ItemKey.asKey(current))) {
                continue;
            }

            store(player, endInv, current.copy(), current.getCount());
            inventory.setItem(slot, ItemStack.EMPTY);
            changed = true;
        }

        if (changed) {
            inventory.setChanged();
        }
    }

    private void tickInternal(ServerPlayer player) {
        // Resynced whether or not the hotbar is mounted: the panel lets the table be
        // curated before any page key is pressed, so the client needs it either way.
        if (++resyncTimer >= RESYNC_INTERVAL_TICKS) {
            resyncTimer = 0;
            resync(player);
        }

        if (enderChest) {
            maintainEnderChest(player);
        } else if (holding) {
            maintainSlots(player);
        }
    }

    public boolean isPanelExpanded() {
        return panelExpanded;
    }

    public void setPanelExpanded(boolean expanded) {
        this.panelExpanded = expanded;
    }


    public void onReturnedToEndless(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        int cell = table.indexOf(ItemKey.asKey(stack));

        if (cell < 0) {
            return;
        }

        int row = cell / HotbarTable.COLUMNS + 1;
        int column = cell % HotbarTable.COLUMNS;
        int[] rows = HotbarGroups.visibleRows(clientGroups, digit);

        for (int i = 0; i < rows.length; i++) {
            if (rows[i] != row) {
                continue;
            }

            int slot = slotFor(i, column);

            if (!player.getInventory().getItem(slot).isEmpty()) {
                return;
            }

            table.remove(cell);
            drawn[slot - EXTENDED_FIRST] = null;
            afterTableChange(player);

            return;
        }
    }

    public void placeOnHotbar(ServerPlayer player, ItemKey key) {
        if (table.place(key) < 0) {
            NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.hotbar_full"));
            return;
        }

        afterTableChange(player);
    }

    public void clearCell(ServerPlayer player, int cell) {
        if (!table.remove(cell)) {
            return;
        }

        vacateCell(player, cell);
        afterTableChange(player);
    }

    private void vacateCell(ServerPlayer player, int cell) {
        if (!holding) {
            return;
        }

        int row = cell / HotbarTable.COLUMNS + 1;
        int column = cell % HotbarTable.COLUMNS;
        int[] rows = HotbarGroups.visibleRows(clientGroups, digit);

        for (int i = 0; i < rows.length; i++) {
            if (rows[i] != row) {
                continue;
            }

            int slot = slotFor(i, column);
            int index = slot - EXTENDED_FIRST;
            ItemKey drawnKey = drawn[index];

            drawn[index] = null;

            if (drawnKey == null) {
                return;
            }

            EndlessInventory endInv = EndlessBridge.forPlayer(player);
            Inventory inventory = player.getInventory();
            ItemStack current = inventory.getItem(slot);

            // Only a stack this cell drew is taken back; anything else is the player's.
            if (endInv == null || current.isEmpty() || !drawnKey.equals(ItemKey.asKey(current))) {
                return;
            }

            store(player, endInv, current.copy(), current.getCount());
            inventory.setItem(slot, ItemStack.EMPTY);
            inventory.setChanged();

            return;
        }
    }

    private void afterTableChange(ServerPlayer player) {
        persistTable(player);
        persistedCells = table.claimedCells();
        sendTable(player);

        if (holding) {
            maintainSlots(player);
        }
    }

    public void sendTable(ServerPlayer player) {
        NetworkHandler.sendToPlayer(player, new HotbarTableS2C(table.cells()));
    }

    private int coveredSlots() {
        if (enderChest) {
            return EXTENDED_COUNT;
        }

        return (HotbarGroups.clampConfigured(clientGroups) - 1) * 9;
    }

    private boolean ownsSlotInternal(int inventorySlot) {
        if (clientGroups < 2) {
            return false;
        }

        return inventorySlot >= EXTENDED_FIRST && inventorySlot < EXTENDED_FIRST + coveredSlots();
    }

    private boolean resizeCoverage(ServerPlayer player, int newCovered) {
        if (newCovered <= heldCovered) {
            for (int i = newCovered; i < heldCovered; i++) {
                drawn[i] = null;
            }

            heldCovered = newCovered;
            return false;
        }

        boolean moved = displace(player, heldCovered, newCovered);
        heldCovered = newCovered;

        return moved;
    }

    private boolean displace(ServerPlayer player, int from, int to) {
        EndlessInventory endInv = EndlessBridge.forPlayer(player);

        if (endInv == null) {
            return false;
        }

        Inventory inventory = player.getInventory();
        boolean changed = false;

        for (int i = from; i < to; i++) {
            int slot = EXTENDED_FIRST + i;
            ItemStack current = inventory.getItem(slot);

            drawn[i] = null;

            if (current.isEmpty()) {
                continue;
            }

            table.suppress(ItemKey.asKey(current));
            store(player, endInv, current.copy(), current.getCount());
            inventory.setItem(slot, ItemStack.EMPTY);
            changed = true;
        }

        if (changed) {
            inventory.setChanged();
        }

        return changed;
    }

    private void toggleEnderChest(ServerPlayer player) {
        // 兜底校验，groups<2直接返回
        if (clientGroups < 2) {
            NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.no_groups"));
            return;
        }
        if (enderChest) {
            leaveEnderChest(player);
            NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.ender_chest_off"));
            return;
        }

        stop(player);

        if (!ERIConfig.ENABLE_ENDER_CHEST_HOTBAR.get()) {
            NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.ender_chest_disabled"));
            return;
        }

        enterEnderChest(player);
        NetworkHandler.sendToPlayer(player, new HotbarNoticeS2C("endless_refined.notice.ender_chest"));
    }

    private void enterEnderChest(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        var chest = player.getEnderChestInventory();

        resizeCoverage(player, EXTENDED_COUNT);
        enderChest = true;

        for (int i = 0; i < EXTENDED_COUNT; i++) {
            ItemStack inChest = chest.getItem(i);

            if (inChest.isEmpty()) {
                continue;
            }

            inventory.setItem(EXTENDED_FIRST + i, inChest);
            chest.setItem(i, ItemStack.EMPTY);
        }

        chest.setChanged();
        inventory.setChanged();
        player.getPersistentData().putBoolean(ENDER_CHEST_TAG, true);
    }

    private void leaveEnderChest(ServerPlayer player) {
        stowIntoEnderChest(player);

        enderChest = false;
        heldCovered = 0;
        resyncTimer = 0;
        digit = 0;
        player.getPersistentData().remove(ENDER_CHEST_TAG);
    }

    private void maintainEnderChest(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        var chest = player.getEnderChestInventory();
        boolean changed = false;

        for (int i = 0; i < EXTENDED_COUNT; i++) {
            if (!inventory.getItem(EXTENDED_FIRST + i).isEmpty()) {
                continue;
            }

            ItemStack inChest = chest.getItem(i);

            if (inChest.isEmpty()) {
                continue;
            }

            inventory.setItem(EXTENDED_FIRST + i, inChest);
            chest.setItem(i, ItemStack.EMPTY);
            changed = true;
        }

        if (changed) {
            chest.setChanged();
            inventory.setChanged();
        }
    }

    private static void stowIntoEnderChest(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        var chest = player.getEnderChestInventory();
        boolean changed = false;

        for (int i = 0; i < EXTENDED_COUNT; i++) {
            ItemStack current = inventory.getItem(EXTENDED_FIRST + i);

            if (current.isEmpty()) {
                continue;
            }

            inventory.setItem(EXTENDED_FIRST + i, storeInto(chest, current));
            changed = true;
        }

        if (changed) {
            chest.setChanged();
            inventory.setChanged();
        }
    }

    private static ItemStack storeInto(Container container, ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);

            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, remaining)) {
                continue;
            }

            int room = Math.min(container.getMaxStackSize(), existing.getMaxStackSize()) - existing.getCount();

            if (room <= 0) {
                continue;
            }

            int moved = Math.min(room, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
        }

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, remaining);
                remaining = ItemStack.EMPTY;
            }
        }

        return remaining;
    }

    private void stop(ServerPlayer player) {
        digit = 0;

        if (holding) {
            displace(player, 0, heldCovered);
        }

        holding = false;
        heldCovered = 0;
        resyncTimer = 0;
        Arrays.fill(drawn, null);
    }

    private void resync(ServerPlayer player) {
        table.sync(EndlessBridge.stockItems(player));

        // Only the layout is worth saving and sending, and only when it changed.
        if (table.claimedCells() != persistedCells) {
            persistTable(player);
            persistedCells = table.claimedCells();
            sendTable(player);
        }
    }

    private void maintainSlots(ServerPlayer player) {
        int[] rows = HotbarGroups.visibleRows(clientGroups, digit);

        if (rows.length == 0) {
            return;
        }

        EndlessInventory endInv = EndlessBridge.forPlayer(player);

        if (endInv == null) {
            return;
        }

        Inventory inventory = player.getInventory();

        // Snapshotted at most once per pass, the first time a count is actually needed.
        Map<ItemKey, ItemState> stock = null;
        boolean changed = false;

        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            int cellBase = (rows[rowIndex] - 1) * HotbarTable.COLUMNS;

            for (int column = 0; column < HotbarTable.COLUMNS; column++) {
                int slot = slotFor(rowIndex, column);
                int index = slot - EXTENDED_FIRST;
                int cell = cellBase + column;
                ItemKey drawnKey = drawn[index];

                if (inventory.getItem(slot).isEmpty()) {
                    if (drawnKey != null) {
                        if (stock == null) {
                            stock = endInv.snapshotItemMap();
                        }

                        // A stack put away into Endless is still there and more of it than
                        // there was, so the player took it off the hotbar on purpose and the
                        // cell goes with it.
                        //
                        // A stack that was used up leaves the count no higher than the
                        // baseline, and the cell is deliberately kept: it is what the refill
                        // below draws into. Clearing it would lose the item's place, and the
                        // replacement would land wherever the next free cell happened to be.
                        if (EndlessBridge.countIn(stock, drawnKey) > drawnStock[index]) {
                            table.remove(cell);
                        }

                        drawn[index] = null;
                        changed = true;
                    }
                } else {
                    ItemKey nowKey = ItemKey.asKey(inventory.getItem(slot));

                    if (drawnKey == null || !drawnKey.equals(nowKey)) {
                        if (stock == null) {
                            stock = endInv.snapshotItemMap();
                        }

                        // The player put this here, so this cell holds it — in this cell, not
                        // in whichever one happened to be free.
                        table.set(cell, nowKey);
                        drawn[index] = nowKey;
                        drawnStock[index] = EndlessBridge.countIn(stock, nowKey);
                        changed = true;
                    }
                }

                if (!inventory.getItem(slot).isEmpty() || drawn[index] != null) {
                    continue;
                }

                if (stock == null) {
                    stock = endInv.snapshotItemMap();
                }

                // A cell whose item has left Endless has nothing left to draw from, so it
                // gives its place up for something else. Only when the slot is empty, which
                // is the case here: with anything in the slot the cell is a record of what
                // the player put on the hotbar, and it may not be an Endless item at all.
                ItemKey cellKey = table.keyAt(cell);

                if (cellKey != null && EndlessBridge.countIn(stock, cellKey) <= 0) {
                    table.clear(cell);
                    changed = true;
                    continue;
                }

                // The refill half is held off while the player has a screen open. They are
                // moving stacks around by hand then, and a slot that refilled itself
                // underneath them could never be emptied — and each attempt would take
                // another stack out of Endless for the trouble. Recording what they do, above,
                // keeps running either way, since that is exactly when they are doing it.
                if (screenOpen) {
                    continue;
                }

                if (checkout(inventory, endInv, stock, rows[rowIndex], column, slot)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            inventory.setChanged();
        }
    }

    private boolean checkout(Inventory inventory, EndlessInventory endInv, Map<ItemKey, ItemState> stock, int row, int column, int slot) {
        ItemKey key = table.keyAt((row - 1) * HotbarTable.COLUMNS + column);

        if (key == null) {
            return false;
        }

        int max = maxStackOf(key);
        int available = EndlessBridge.countIn(stock, key);

        if (available <= 0) {
            return false;
        }

        ItemStack taken = endInv.takeItem(key, Math.min(available, max));

        if (taken.isEmpty()) {
            return false;
        }

        // Infinity mode hands back the whole pool rather than what was asked for.
        if (taken.getCount() > max) {
            taken.setCount(max);
        }

        int index = slot - EXTENDED_FIRST;

        inventory.setItem(slot, taken);
        drawn[index] = ItemKey.asKey(taken);
        // What is left behind, which is the baseline an emptied slot is judged against. In
        // infinity mode nothing was taken, so this stays as high as it was.
        drawnStock[index] = Math.max(0, available - taken.getCount());

        return true;
    }

    private void store(ServerPlayer player, EndlessInventory endInv, ItemStack stack, int count) {
        if (count <= 0) {
            return;
        }

        ItemStack remainder = endInv.addItem(ItemKey.asKey(stack), count);

        if (!remainder.isEmpty()) {
            LOGGER.warn("Endless Inventory refused {} x {}, dropping it rather than destroying it", remainder.getCount(), remainder.getItem());

            player.drop(remainder, false);
        }
    }

    private static int maxStackOf(ItemKey key) {
        return Math.max(1, key.toStack(1).getMaxStackSize());
    }

    private void persistTable(ServerPlayer player) {
        table.save(player.getPersistentData());
    }
}
