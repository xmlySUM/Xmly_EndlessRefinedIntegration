package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.util.ItemKey;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EndInvAffinities {

    public static final Logger LOGGER = LogUtils.getLogger();

    public final List<ItemStack> starredItems = new ArrayList<>();
    public final EndlessInventory endInv;
    public final UUID endInvUUID;

    public EndInvAffinities(EndlessInventory endInv){
        this.endInv = endInv;
        this.endInvUUID = endInv.getUuid();
    }

    public void addStarredItem(ItemStack stack){
        if(stack.isEmpty()) return;
        ItemKey key = ItemKey.asKey(stack).canonical();
        for(ItemStack item : starredItems){
            if(ItemKey.asKey(item).canonical().equals(key)){
                return;
            }
        }
        starredItems.add(stack.copyWithCount(1));
    }

    public void removeStarredItem(ItemStack stack){
        if (stack.isEmpty()) return;
        //Match by key rather than by stack: a starred item that arrived without its tag - or with an
        //empty one - could otherwise never be found again and so could never be unstarred.
        ItemKey key = ItemKey.asKey(stack).canonical();
        starredItems.removeIf(item -> ItemKey.asKey(item).canonical().equals(key));
    }

    /**
     * Get list of starred items of EndInv.
     * @param startIndex the startIndex of sublist
     * @param length the length of sublist, if too big, will return whole or just to last.
     * @return the sublist of starred items without copy.
     */
    public List<ItemStack> getStarredItems(@Nonnegative int startIndex,@Nonnegative int length){
        if(length >= starredItems.size()) return starredItems;
        if(startIndex+length > starredItems.size()) return starredItems.subList(startIndex,-1);
        return starredItems.subList(startIndex,startIndex+length);
    }
}
