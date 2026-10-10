package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.util.ItemKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface IPlatform {

    boolean onItemStackedOn(ItemStack clickedItem, ItemStack carriedItem, Slot slot, ClickAction action, Player player, SlotAccess access);

    /**Called when a click on the Endless Inventory page reached the server, before the page acts
     * on it.<br>
     * The hotbar needs this: while its panel is open a quick-move means "put this item on the
     * hotbar", not "put it in the container that happens to be open".
     * @return true when the click has been dealt with and the page must leave it alone
     */
    default boolean onPageClicked(ServerPlayer player, ItemKey key, ClickType clickType) {
        return false;
    }
}
