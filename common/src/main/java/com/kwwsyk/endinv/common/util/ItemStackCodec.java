package com.kwwsyk.endinv.common.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**Item encoding that keeps everything an item carries.<br>
 * Two things are easy to lose and this does not lose either of them.
 * <p>{@link FriendlyByteBuf#writeItem(ItemStack)} only writes the tag of an item that
 * {@link Item#canBeDepleted() can be depleted} or {@link Item#shouldOverrideMultiplayerNbt()
 * overrides its multiplayer nbt}; every other stack loses its tag on the wire. Renamed items,
 * enchanted books and anything else carrying custom data therefore arrived on the other side as
 * plain items.
 * <p>Forge also keeps an item's capability data <em>beside</em> its tag rather than inside it,
 * under {@code ForgeCaps}, where {@link ItemStack#getTag()} does not look. Sending the tag alone
 * dropped all of it. Both are carried here.
 */
public final class ItemStackCodec {

    private ItemStackCodec() {
    }

    public static void writeStack(FriendlyByteBuf o, ItemStack stack){
        if(stack==null || stack.isEmpty()){
            o.writeBoolean(false);
            return;
        }
        o.writeBoolean(true);
        //The whole stack, rather than its tag: this is the one form that includes ForgeCaps.
        o.writeNbt(stack.serializeNBT());
    }

    public static ItemStack readStack(FriendlyByteBuf o){
        if(!o.readBoolean()) return ItemStack.EMPTY;
        CompoundTag nbt = o.readNbt();
        return nbt==null ? ItemStack.EMPTY : fromNbt(nbt);
    }

    /**A stack from its serialized form, capability data included.<br>
     * {@link ItemStack#of(CompoundTag)} restores the item, its count and its tag but not the
     * capability data Forge keeps beside the tag, so that is put back separately.
     */
    public static ItemStack fromNbt(CompoundTag nbt){
        ItemStack stack = ItemStack.of(nbt);
        if(nbt.contains("ForgeCaps")) stack.deserializeNBT(nbt);
        return stack;
    }

    /**Writes an {@link ItemKey} as its item, tag and capability data.<br>
     * Count is deliberately not written: a key carries identity, not amount.
     */
    public static void writeKey(FriendlyByteBuf o, ItemKey key){
        o.writeVarInt(BuiltInRegistries.ITEM.getId(key.item()));
        o.writeNbt(key.tag());
        o.writeNbt(key.caps());
    }

    public static ItemKey readKey(FriendlyByteBuf o){
        Item item = BuiltInRegistries.ITEM.byId(o.readVarInt());
        CompoundTag tag = o.readNbt();
        CompoundTag caps = o.readNbt();
        return new ItemKey(item==null?Items.AIR:item,tag,caps);
    }

    /**Writes an {@link ItemStackLike} including its count.<br>
     * A count of zero is meaningful here - {@link ItemStackLike} doubles as a key with an amount -
     * so this does not go through {@link #writeStack}, which would collapse it to an empty stack.
     */
    public static void writeStackLike(FriendlyByteBuf o, ItemStackLike item){
        o.writeVarInt(BuiltInRegistries.ITEM.getId(item.item()));
        o.writeInt(item.count());
        o.writeNbt(item.tag());
        o.writeNbt(item.caps());
    }

    public static ItemStackLike readStackLike(FriendlyByteBuf o){
        Item item = BuiltInRegistries.ITEM.byId(o.readVarInt());
        int count = o.readInt();
        CompoundTag tag = o.readNbt();
        CompoundTag caps = o.readNbt();
        return new ItemStackLike(item==null?Items.AIR:item,count,tag,caps);
    }
}
