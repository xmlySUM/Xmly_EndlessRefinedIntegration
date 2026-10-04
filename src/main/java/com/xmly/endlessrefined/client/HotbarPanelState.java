package com.xmly.endlessrefined.client;

import com.xmly.endlessrefined.hotbar.HotbarTable;
import com.xmly.endlessrefined.hotbar.engine.HotbarEngineConfig;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class HotbarPanelState {
    private static List<ItemStack> cells = emptyCells();

    private HotbarPanelState() {
    }

    public static ItemStack cellAt(int index) {
        return index >= 0 && index < cells.size() ? cells.get(index) : ItemStack.EMPTY;
    }

    public static void setCells(List<ItemStack> newCells) {
        cells = List.copyOf(newCells);
    }

    public static boolean isExpanded() {
        return HotbarEngineConfig.panelExpanded;
    }

    public static void toggleExpanded() {
        HotbarEngineConfig.setPanelExpanded(!HotbarEngineConfig.panelExpanded);
    }

    private static List<ItemStack> emptyCells() {
        List<ItemStack> empty = new ArrayList<>(HotbarTable.CELLS);
        for (int i = 0; i < HotbarTable.CELLS; i++) {
            empty.add(ItemStack.EMPTY);
        }
        return empty;
    }
}
