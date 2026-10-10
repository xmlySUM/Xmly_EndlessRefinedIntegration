package com.kwwsyk.endinv.common.client.gui.page;

import com.kwwsyk.endinv.common.client.CachedSrcInv;
import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.gui.page.manager.PageManager;
import com.kwwsyk.endinv.common.client.option.CachedConfig;
import com.kwwsyk.endinv.common.menu.page.PageType;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;

/**Page that displays EndInv's items (directly {@link CachedSrcInv})
 *
 */
public class ItemDisplay extends ItemPage{

    private static final Logger LOGGER = LogUtils.getLogger();

    public ItemDisplay(PageType pageType, PageManager metaDataManager) {
        super(pageType,metaDataManager);
    }

    public void refreshItems(){
        requestRemoteContents();
        readCachedItems();
    }

    /**Redraw from the client's own copy of the inventory, without a word to the server. */
    @Override
    protected void refreshItemsOptimistically(){
        readCachedItems();
    }

    /**
     * Read items data from {@link CachedSrcInv} when transfer mode is {@code ALL} and rebuild the view.
     */
    public void readCachedItems(){
        List<ItemPointer> view = CachedSrcInv.INSTANCE.getItemView(startIndex,length,
                CachedConfig.sortType(), CachedConfig.reverseSort(),
                getClassify(), CachedConfig.searching());
        traceTags(view);
        buildContentsWith(view);
    }

    /**Say how much item data the view it is about to draw actually has, when the client's debug
     * switch is on. The other half of this is logged when the content arrives, so between the two
     * it is clear whether a tag was lost on the wire or later.
     */
    private static void traceTags(List<ItemPointer> view){
        if(!ClientModInfo.getClientConfig().screenDebugging().get()) return;
        long tagged = view.stream()
                .map(ItemPointer::key)
                .filter(key -> key.tag()!=null && !key.tag().isEmpty())
                .count();
        LOGGER.info("[endinv nbt] page view built: {} shown, {} carrying a tag", view.size(), tagged);
    }

    public void requestRemoteContents(){
        sendChangesToServer();
    }

    /**
     * Build Displayed view with a ItemStack list.
     * @param stacks itemstack list to fill the view
     */
    public void buildContentsWith(@NotNull List<ItemPointer> stacks){
        if(holdOn){
            inQueueStacks = stacks;
            return;
        }
        for(int i=0; i<this.length; ++i){
            if(i<stacks.size() && stacks.get(i) != null) {
                this.items.set(i, stacks.get(i));
            }else {
                this.items.set(i,ItemPointer.EMPTY);
            }
        }
    }

    @Override
    public boolean hasSearchbox() {
        return true;
    }

    @Override
    public boolean hasSortTypeSwitchBar() {
        return true;
    }

    public void release(){
        if(holdOn){
            holdOn = false;
            if(inQueueStacks==null) return;
            buildContentsWith(inQueueStacks);
        }
    }
}
