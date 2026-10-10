package com.kwwsyk.endinv.common.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record ItemState(int count, long lastModTime) {


    public static void encode(FriendlyByteBuf o,ItemState state){
        o.writeInt(state.count);
        o.writeLong(state.lastModTime);
    }

    public static ItemState decode(FriendlyByteBuf o){
        return new ItemState(o.readInt(),o.readLong());
    }

    /**The item this state stands for, with its tag.<br>
     * Goes through {@link ItemKey#toStack(int)} rather than {@code new ItemStack(item, count, tag)}.
     * That three argument constructor is Forge's, and its third argument is the <em>capability</em>
     * NBT, not the item's tag: it fills {@code capNBT} and leaves {@code tag} null. Every stack
     * built that way came out plain, which is why items with NBT - enchanted books, potions,
     * renamed things - were drawn without their enchantments and their tooltips without their
     * lines, however well the data itself had survived the trip.
     */
    public ItemStack toStack(ItemKey key) {
        return key.toStack(count);
    }
}
