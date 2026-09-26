package com.xmly.endlessrefined.network;

import com.kwwsyk.endinv.common.util.ItemKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class KeyCodec {

    private KeyCodec() {
    }

    public static void writeStack(FriendlyByteBuf buffer, ItemStack stack) {
        if (stack.isEmpty()) {
            buffer.writeBoolean(false);
            return;
        }

        buffer.writeBoolean(true);
        buffer.writeVarInt(BuiltInRegistries.ITEM.getId(stack.getItem()));
        buffer.writeInt(stack.getCount());
        buffer.writeNbt(stack.getTag());
    }

    public static ItemStack readStack(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.byId(buffer.readVarInt());
        ItemStack stack = new ItemStack(item == null ? Items.AIR : item, buffer.readInt());

        stack.setTag(buffer.readNbt());

        return stack;
    }

    public static void write(FriendlyByteBuf buffer, ItemKey key) {
        buffer.writeVarInt(BuiltInRegistries.ITEM.getId(key.item()));
        buffer.writeNbt(key.tag());
    }

    public static ItemKey read(FriendlyByteBuf buffer) {
        Item item = BuiltInRegistries.ITEM.byId(buffer.readVarInt());

        return new ItemKey(item == null ? Items.AIR : item, buffer.readNbt());
    }
}
