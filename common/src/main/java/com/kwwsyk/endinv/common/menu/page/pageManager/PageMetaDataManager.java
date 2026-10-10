package com.kwwsyk.endinv.common.menu.page.pageManager;

import com.kwwsyk.endinv.common.SourceInventory;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.network.payloads.PageData;
import com.kwwsyk.endinv.common.util.SortType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 *
 */
public interface PageMetaDataManager {

    AbstractContainerMenu getMenu();

    SourceInventory getSourceInventory();

    Player getPlayer();

    void switchPageWithIndex(int index);

    int rows();

    int columns();

    int getItemSize();

    int getMaxStackSize();

    boolean enableInfinity();

    default ItemStack quickMoveFromPage(ItemStack stack){
        return new PageQuickMoveHandler(getMenu()).quickMoveFromPage(stack);
    }

    SortType sortType();

    void setSortType(SortType sortType);

    boolean isSortReversed();

    default void switchSortReversed(){
        setSortReversed(!isSortReversed());
    }

    void setSortReversed(boolean reversed);

    String searching();

    void setSearching(String searching);

    void sendEndInvData();

    String getDisplayingPageId();

    void switchPageWithId(String id);

    PageType getDisplayingPageType();

    default PageData getPageData(){
        return new PageData(getDisplayingPageId(), rows(), columns(),sortType(),isSortReversed(),searching());
    }

}
