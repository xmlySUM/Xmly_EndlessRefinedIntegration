package com.kwwsyk.endinv.common.client.gui.page.manager;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.gui.page.DisplayPage;
import com.kwwsyk.endinv.common.client.gui.page.ItemPage;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.kwwsyk.endinv.common.menu.page.PageTypeRegistry;
import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import com.kwwsyk.endinv.common.network.payloads.toServer.ItemPageContext;
import com.kwwsyk.endinv.common.util.SortType;

import java.util.List;
import java.util.Objects;
//client
public interface PageManager extends PageMetaDataManager {

    List<DisplayPage> getPages();

    DisplayPage getDisplayingPage();

    default void scrollTo(float pos){
        getDisplayingPage().scrollTo(pos);
    }

    default int getDisplayingPageIndex(){
        for(int i=0; i<getPages().size(); ++i){
            if(getPages().get(i)==getDisplayingPage()){
                return i;
            }
        }
        return -1;
    }

    default void switchPageWithId(String id){
        for(int i=0; i<getPages().size(); ++i){
            if(Objects.equals(getPages().get(i).id,id)){
                switchPageWithIndex(i);
            }
        }
    }

    /**
     * the return value of {@link #getPages()} shall be from this.
     */
    default List<DisplayPage> buildPages(){
        return PageTypeRegistry.getDisplayPages().stream().map(type -> type.buildPage(this)).toList();
    }

    default ItemPageContext getInPageContext(){
        DisplayPage page = getDisplayingPage();
        return new ItemPageContext(
                page instanceof ItemPage itemPage ? itemPage.getStartIndex() : 0,
                rows()* columns(),
                getPageData()
        );
    }

    default String getDisplayingPageId(){
        return getDisplayingPage().id;
    }

    default PageType getDisplayingPageType(){
        return getDisplayingPage().getPageType();
    }

    @Override
    default int getItemSize() {
        return CachedSrcInv.INSTANCE.getItemSize();
    }

    @Override
    default int getMaxStackSize() {
        return CachedSrcInv.INSTANCE.getMaxItemStackSize();
    }

    @Override
    default boolean enableInfinity() {
        return CachedSrcInv.INSTANCE.isInfinityMode();
    }

    @Override
    default SortType sortType() {
        return CachedConfig.sortType();
    }

    @Override
    default void setSortType(SortType sortType) {
        CachedConfig.setSortType(sortType);
    }

    @Override
    default boolean isSortReversed() {
        return CachedConfig.reverseSort();
    }

    @Override
    default void setSortReversed(boolean reversed) {
        CachedConfig.setReverseSort(reversed);
    }

    @Override
    default String searching() {
        return CachedConfig.searching();
    }

    @Override
    default void setSearching(String searching) {
        CachedConfig.setSearching(searching);
    }

    @Override
    default void sendEndInvData() {//do nop as in the client
    }
}