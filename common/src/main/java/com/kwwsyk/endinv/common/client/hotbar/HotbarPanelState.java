package com.kwwsyk.endinv.common.client.hotbar;

import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.hotbar.HotbarTable;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**The client's copy of the hotbar table, as far as the panel is concerned.<br>
 * Only the item in each cell is kept; the amounts are the stacks sitting in the hotbar slots.
 * This lives in {@code common} because the EndInv page has to know whether the panel is open: with
 * it open a quick-move puts the item on the hotbar instead of into the container, and the client
 * must not run ahead of the server and move it locally.
 */
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
        return ClientModInfo.getClientConfig().hotbarPanelExpanded().get();
    }

    public static void toggleExpanded() {
        IClientConfig config = ClientModInfo.getClientConfig();
        config.hotbarPanelExpanded().set(!config.hotbarPanelExpanded().get());
        config.save();
    }

    private static List<ItemStack> emptyCells() {
        List<ItemStack> empty = new ArrayList<>(HotbarTable.CELLS);
        for (int i = 0; i < HotbarTable.CELLS; i++) {
            empty.add(ItemStack.EMPTY);
        }
        return empty;
    }
}
