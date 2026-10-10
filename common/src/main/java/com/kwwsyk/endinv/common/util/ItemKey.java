package com.kwwsyk.endinv.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Objects;

/**What makes one stack the same as another: the item, its tag, and its Forge capabilities.<br>
 * That last one is easy to miss. Forge keeps an item's capability data <em>beside</em> its tag,
 * under {@code ForgeCaps}, and {@link ItemStack#getTag()} does not contain it. An item whose data
 * lives in a capability - which is where a good many mods put it - therefore looked like a plain
 * item to anything that only asked for the tag, and lost that data on the way in.
 */
public record ItemKey(Item item, CompoundTag tag, @Nullable CompoundTag caps) {

    public static final ItemKey EMPTY = new ItemKey(Items.AIR, null, null);

    /**A key for an item that carries no capability data, which is almost every item. */
    public ItemKey(Item item, CompoundTag tag) {
        this(item, tag, null);
    }

    public static void encode(FriendlyByteBuf o,ItemKey key){
        ItemStackCodec.writeKey(o,key);
    }

    public static ItemKey decode(FriendlyByteBuf O) {
        return ItemStackCodec.readKey(O);
    }

    public boolean isEmpty(){
        return Objects.equals(item, Items.AIR);
    }

    public ItemStack toStack(int count){
        ItemStack stack = new ItemStack(item,count);
        stack.setTag(copyTag(tag));
        applyCaps(stack,caps);
        return stack;
    }

    public static ItemKey asKey(ItemStack stack){
        return new ItemKey(stack.getItem(),copyTag(stack.getTag()),copyTag(capsOf(stack)));
    }

    /**The same key with an absent tag or capability data and an empty one spelled the same way.<br>
     * Some mods hand out stacks whose tag is an empty {@link CompoundTag} where the same item
     * elsewhere has none at all. Because record equality compares by content, those two spellings
     * of one item do not match and the store ends up with two entries for it.
     * @return this key, or an equal one with those parts either absent or non-empty
     */
    public ItemKey canonical(){
        CompoundTag tagOut = tag==null || tag.isEmpty() ? null : tag;
        CompoundTag capsOut = caps==null || caps.isEmpty() ? null : caps;
        if(tagOut==tag && capsOut==caps) return this;
        return new ItemKey(item,tagOut,capsOut);
    }

    /**Forge's capability data for a stack, taken from the one place it is written to. */
    @Nullable
    static CompoundTag capsOf(ItemStack stack){
        CompoundTag caps = stack.serializeNBT().getCompound("ForgeCaps");
        return caps.isEmpty() ? null : caps;
    }

    /**Put capability data back on a stack, through the same round trip Forge reads it with. */
    static void applyCaps(ItemStack stack, @Nullable CompoundTag caps){
        if(caps==null || caps.isEmpty()) return;
        CompoundTag full = stack.serializeNBT();
        full.put("ForgeCaps",caps.copy());
        stack.deserializeNBT(full);
    }

    /**A stack's tag is handed to the caller rather than copied, so a key built from a stack would
     * share a mutable tag with it. Editing either side would change the other's hash, which is how
     * a key disappears from the map it was put in.
     */
    private static CompoundTag copyTag(CompoundTag tag){
        return tag==null ? null : tag.copy();
    }
}
