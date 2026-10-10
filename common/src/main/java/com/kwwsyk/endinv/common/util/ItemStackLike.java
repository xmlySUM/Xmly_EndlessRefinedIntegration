package com.kwwsyk.endinv.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**An item, an amount, and everything the item carries.<br>
 * The capability data is carried for the same reason the tag is: Forge keeps it beside the tag
 * rather than inside it, so an item whose data lives in a capability looks like a plain item to
 * anyone who only asks for the tag.
 */
public record ItemStackLike(Item item, int count, CompoundTag tag, @Nullable CompoundTag caps) {

    /**For an item that carries no capability data, which is almost every item. */
    public ItemStackLike(Item item, int count, CompoundTag tag) {
        this(item, count, tag, null);
    }

    public static void encode(FriendlyByteBuf o,ItemStackLike item){
        ItemStackCodec.writeStackLike(o,item);
    }

    public static ItemStackLike decode(FriendlyByteBuf o){
        return ItemStackCodec.readStackLike(o);
    }

    public static ItemStackLike asKey(ItemStack stack){
        return new ItemStackLike(stack.getItem(),0,copyTag(stack.getTag()),copyTag(ItemKey.capsOf(stack)));
    }

    public static ItemStackLike asKey(ItemStack stack, int count){
        return new ItemStackLike(stack.getItem(),count,copyTag(stack.getTag()),copyTag(ItemKey.capsOf(stack)));
    }

    public static ItemStackLike of(ItemStack stack, int count){
        return asKey(stack,count);
    }

    public ItemStack toKey(){
        var ret = new ItemStack(item,count);
        ret.setTag(copyTag(tag));
        ItemKey.applyCaps(ret,caps);
        return ret;
    }

    private static CompoundTag copyTag(CompoundTag tag){
        return tag==null ? null : tag.copy();
    }
}
